package edu.zsc.ai.domain.service.oauth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.common.constant.JwtClaimConstant;
import edu.zsc.ai.common.enums.sys.SessionStatusEnum;
import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;
import edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.service.sys.SysRefreshTokensService;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.domain.service.sys.SysUsersService;
import edu.zsc.ai.util.CryptoUtil;
import edu.zsc.ai.util.HttpRequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    public String getAuthorizationUrl(String provider) {
        String state = "state-" + System.currentTimeMillis(); // Simple state generation
        return oAuthStrategyFactory.getStrategy(provider).getAuthorizationUrl(state);
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenPairResponse login(String provider, String code) {
        // 1. Get OAuth User Info
        OAuthStrategy strategy = oAuthStrategyFactory.getStrategy(provider);
        OAuthUserInfo userInfo = strategy.getUserInfo(code);

        // 2. Find or Register User
        SysUsers user = sysUsersService.getOne(new LambdaQueryWrapper<SysUsers>()
                .eq(SysUsers::getEmail, userInfo.getEmail()));

        if (user == null) {
            user = new SysUsers();
            user.setEmail(userInfo.getEmail());
            // Generate a unique username if possible, or use email prefix
            String username = userInfo.getNickname();
            if (username == null || username.isEmpty()) {
                username = userInfo.getEmail().split("@")[0];
            }
            // Append timestamp to ensure uniqueness
            user.setUsername(username + "_" + System.currentTimeMillis());
            user.setAvatarUrl(userInfo.getAvatarUrl());
            user.setAuthProvider(provider.toUpperCase());
            user.setVerified(true); // OAuth users are usually verified
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            sysUsersService.save(user);
        } else {
            // Update existing user info if necessary
            if (user.getAvatarUrl() == null && userInfo.getAvatarUrl() != null) {
                user.setAvatarUrl(userInfo.getAvatarUrl());
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

        return new TokenPairResponse(accessToken, refreshTokenPlain);
    }
}
