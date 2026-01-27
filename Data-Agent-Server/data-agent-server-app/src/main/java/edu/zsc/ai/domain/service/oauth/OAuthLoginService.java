package edu.zsc.ai.domain.service.oauth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.common.constant.JwtClaimConstant;
import edu.zsc.ai.common.enums.sys.SessionStatusEnum;
import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;
import edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.service.sys.SysRefreshTokensService;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.domain.service.sys.SysUsersService;
import edu.zsc.ai.util.CryptoUtil;
import edu.zsc.ai.util.HttpRequestUtil;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for OAuth login operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthLoginService {

    private final OAuthStrategyFactory oAuthStrategyFactory;
    private final SysUsersService sysUsersService;
    private final SysSessionsService sysSessionsService;
    private final SysRefreshTokensService sysRefreshTokensService;

    public String getAuthorizationUrl(String fromUrl, String provider) {
        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((fromUrl + "." + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        return oAuthStrategyFactory.getStrategy(provider).getAuthorizationUrl(state);
    }

    /**
     * Handle OAuth callback and return redirect URL
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String provider, String code, String state, String error) {
        try {
            // Get OAuth strategy instance
            OAuthStrategy strategy = oAuthStrategyFactory.getStrategy(provider);

            // Extract fromUrl from state parameter
            String fromUrl = strategy.extractFromUrlFromState(state);
            if (fromUrl == null) {
                log.error("Failed to extract fromUrl from state parameter");
                throw new BusinessException("error.oauth.state.invalid");
            }

            // Validate callback parameters
            if (!strategy.validateCallbackParams(code, state, error)) {
                return strategy.buildErrorRedirectUrl(fromUrl, "invalid_parameters");
            }

            // 1. Get OAuth User Info
            OAuthUserInfo userInfo = strategy.getUserInfo(code);

            // 2. Find or Register User
            SysUsers user = sysUsersService.getOne(new LambdaQueryWrapper<SysUsers>()
                    .eq(SysUsers::getEmail, userInfo.getEmail()));

            if (user == null) {
                user = new SysUsers();
                user.setEmail(userInfo.getEmail());

                // Generate stable username based on provider and nickname
                String username = userInfo.getNickname();
                if (username == null || username.isEmpty()) {
                    username = userInfo.getEmail().split("@")[0];
                }

                // Make username safe (only letters, numbers, underscore)
                username = username.replaceAll("[^a-zA-Z0-9_]", "_");

                // If username already exists, append provider short name
                SysUsers existingUser = sysUsersService.getOne(new LambdaQueryWrapper<SysUsers>()
                        .eq(SysUsers::getUsername, username));
                if (existingUser != null) {
                    String providerShort = "gh".equalsIgnoreCase(provider) ? "gh" : "gg";
                    username = username + "_" + providerShort;
                }

                user.setUsername(username);
                user.setAvatarUrl(userInfo.getAvatarUrl());
                user.setAuthProvider(provider);
                user.setVerified(true); // OAuth users are usually verified

                // Generate random password for OAuth users (they won't use it, but DB requires
                // it)
                String randomPassword = CryptoUtil.generateRandomPassword();
                user.setPasswordHash(CryptoUtil.encode(randomPassword));

                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                sysUsersService.save(user);
            } else {
                // Update existing user info if necessary
                if (user.getAvatarUrl() == null && userInfo.getAvatarUrl() != null) {
                    user.setAvatarUrl(userInfo.getAvatarUrl());
                    user.setUpdatedAt(LocalDateTime.now());
                    sysUsersService.updateById(user);
                }
            }

            // 3. Login (Sa-Token)
            SaLoginModel model = new SaLoginModel();
            model.setExtra(JwtClaimConstant.USERNAME, user.getUsername());
            model.setExtra(JwtClaimConstant.EMAIL, user.getEmail());
            model.setExtra(JwtClaimConstant.AVATAR_URL, user.getAvatarUrl());
            StpUtil.login(user.getId(), model);
            String accessToken = StpUtil.getTokenValue();

            // 4. Create Session
            String ip = HttpRequestUtil.extractClientIp();
            String userAgent = HttpRequestUtil.extractUserAgent();

            SysSessions session = new SysSessions();
            session.setUserId(user.getId());
            session.setAccessTokenHash(CryptoUtil.sha256Hex(accessToken));
            session.setIpAddress(ip);
            session.setUserAgent(userAgent);
            session.setActive(SessionStatusEnum.ACTIVE.getValue());
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            session.setLastRefreshAt(LocalDateTime.now());
            sysSessionsService.save(session);

            // 5. Create Refresh Token
            CreateRefreshTokenRequest createReq = new CreateRefreshTokenRequest();
            createReq.setUserId(user.getId());
            createReq.setSessionId(session.getId());
            String refreshTokenPlain = sysRefreshTokensService.createAndStoreRefreshToken(createReq);

            // Build success redirect URL
            return strategy.buildSuccessRedirectUrl(
                    fromUrl,
                    accessToken,
                    refreshTokenPlain);

        } catch (BusinessException e) {
            // Business exceptions should be handled by GlobalExceptionHandler
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred while processing OAuth callback", e);
            // Try to extract fromUrl from state for error redirect
            try {
                OAuthStrategy strategy = oAuthStrategyFactory.getStrategy(provider);
                String fromUrl = strategy.extractFromUrlFromState(state);
                if (fromUrl != null) {
                    return strategy.buildErrorRedirectUrl(fromUrl, "callback_processing_failed");
                }
            } catch (Exception ex) {
                log.error("Failed to extract fromUrl for error redirect", ex);
            }
            throw new BusinessException("error.oauth.callback.processing_failed");
        }
    }
}
