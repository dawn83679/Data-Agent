package edu.zsc.ai.domain.service.sys.iml;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.common.constant.JwtClaimConstant;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.enums.sys.SessionStatusEnum;
import edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.FindSessionByTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.LoginRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RegisterRequest;
import edu.zsc.ai.domain.model.dto.request.sys.ResetPasswordRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RevokeRefreshTokenBySessionRequest;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;
import edu.zsc.ai.domain.model.dto.response.sys.UserResponse;
import edu.zsc.ai.domain.model.entity.sys.SysRefreshTokens;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.model.enums.RefreshTokenStatusEnum;
import edu.zsc.ai.domain.service.sys.AuthService;
import edu.zsc.ai.domain.service.sys.SysRefreshTokensService;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.domain.service.sys.SysUsersService;
import edu.zsc.ai.util.ConditionalUtil;
import edu.zsc.ai.util.CryptoUtil;
import edu.zsc.ai.util.HttpRequestUtil;
import edu.zsc.ai.util.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUsersService sysUsersService;

    @Autowired
    private SysSessionsService sysSessionsService;

    @Autowired
    private SysRefreshTokensService sysRefreshTokensService;

    @Override
    public UserResponse getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUsers user = sysUsersService.getById(userId);
        BusinessException.throwIf(user == null, ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.USER_NOT_FOUND_MESSAGE);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .verified(user.getVerified())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public TokenPairResponse loginByEmail(LoginRequest request) {
        // 1. Query user
        SysUsers user = sysUsersService.getOne(new LambdaQueryWrapper<SysUsers>()
                .eq(SysUsers::getEmail, request.getEmail()));

        BusinessException.throwIf(user == null,
                ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.USER_NOT_FOUND_MESSAGE);

        // 2. Validate password
        BusinessException.throwIf(!CryptoUtil.match(request.getPassword(), user.getPasswordHash()),
                ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.INVALID_CREDENTIALS_MESSAGE);

        // 3. Login and generate JWT
        SaLoginModel model = new SaLoginModel();
        model.setExtra(JwtClaimConstant.USERNAME, user.getUsername());
        model.setExtra(JwtClaimConstant.EMAIL, user.getEmail());
        model.setExtra(JwtClaimConstant.AVATAR_URL, user.getAvatarUrl());
        StpUtil.login(user.getId(), model);
        String accessToken = StpUtil.getTokenValue();

        // 4. Extract IP and UA
        String ip = HttpRequestUtil.extractClientIp();
        String userAgent = HttpRequestUtil.extractUserAgent();

        // 5. Persist session
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

        // 6. Generate and persist refresh token
        CreateRefreshTokenRequest createReq = new CreateRefreshTokenRequest();
        createReq.setUserId(user.getId());
        createReq.setSessionId(session.getId());
        String refreshTokenPlain = sysRefreshTokensService.createAndStoreRefreshToken(createReq);

        return new TokenPairResponse(accessToken, refreshTokenPlain);
    }

    @Override
    public TokenPairResponse refreshToken(String refreshTokenPlain) {
        // 1. Verify refresh token validity
        SysRefreshTokens refreshToken = sysRefreshTokensService.verifyAndGet(refreshTokenPlain);
        BusinessException.throwIf(refreshToken == null, ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.INVALID_REFRESH_TOKEN_MESSAGE);

        // 2. Revoke old refresh token
        sysRefreshTokensService.revoke(refreshTokenPlain);

        // 3. Get user information
        SysUsers user = sysUsersService.getById(refreshToken.getUserId());
        BusinessException.throwIf(user == null, ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.USER_NOT_FOUND_MESSAGE);

        // 4. Generate new access token
        StpUtil.logout();
        StpUtil.login(user.getId(), new SaLoginModel()
                .setExtra(JwtClaimConstant.USERNAME, user.getUsername())
                .setExtra(JwtClaimConstant.EMAIL, user.getEmail())
                .setExtra(JwtClaimConstant.AVATAR_URL, user.getAvatarUrl()));
        String accessToken = StpUtil.getTokenValue();

        // 5. Update session information
        SysSessions session = sysSessionsService.getById(refreshToken.getSessionId());
        ConditionalUtil.ifNotNull(session, s -> {
            s.setAccessTokenHash(CryptoUtil.sha256Hex(accessToken));
            s.setLastRefreshAt(LocalDateTime.now());
            sysSessionsService.updateById(s);
        });

        // 6. Generate new refresh token
        CreateRefreshTokenRequest createReq = new CreateRefreshTokenRequest();
        createReq.setUserId(user.getId());
        createReq.setSessionId(refreshToken.getSessionId());
        String newRefreshTokenPlain = sysRefreshTokensService.createAndStoreRefreshToken(createReq);

        // 7. Return new token pair
        return new TokenPairResponse(accessToken, newRefreshTokenPlain);
    }

    @Override
    public Boolean register(RegisterRequest request) {
        // Validate username/email globally unique
        BusinessException.throwIf(
                sysUsersService
                        .exists(new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getUsername,
                                request.getUsername())),
                ResponseCode.PARAM_ERROR,
                ResponseMessageKey.EMAIL_OR_USERNAME_EXISTS_MESSAGE);
        BusinessException.throwIf(
                sysUsersService.exists(new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getEmail,
                        request.getEmail())),
                ResponseCode.PARAM_ERROR,
                ResponseMessageKey.EMAIL_OR_USERNAME_EXISTS_MESSAGE);

        SysUsers user = new SysUsers();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(CryptoUtil.encode(request.getPassword()));
        user.setVerified(true); // Default verified for now
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return sysUsersService.save(user);
    }

    @Override
    public Boolean logout() {
        // 1. Get current logged-in user ID
        long loginUserId = StpUtil.getLoginIdAsLong();

        // 2. Get current session
        String accessToken = StpUtil.getTokenValue();

        FindSessionByTokenRequest findReq = new FindSessionByTokenRequest();
        findReq.setAccessToken(accessToken);
        findReq.setUserId(loginUserId);
        SysSessions session = sysSessionsService.findByAccessTokenAndUserId(findReq);

        // 3. Mark session as inactive and revoke related tokens
        ConditionalUtil.ifNotNull(session, s -> {
            s.setActive(SessionStatusEnum.INACTIVE.getValue());
            sysSessionsService.updateById(s);

            // 4. Revoke all refresh tokens associated with this session
            RevokeRefreshTokenBySessionRequest revokeReq = new RevokeRefreshTokenBySessionRequest();
            revokeReq.setSessionId(s.getId());
            sysRefreshTokensService.revokeAllBySessionId(revokeReq);
        });
        StpUtil.logout();
        return true;
    }

    @Override
    public Boolean resetPassword(ResetPasswordRequest request) {
        // 1) Query user by email
        SysUsers user = sysUsersService.getOne(new LambdaQueryWrapper<SysUsers>()
                .eq(SysUsers::getEmail, request.getEmail()));
        BusinessException.throwIf(user == null,
                ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.USER_NOT_FOUND_MESSAGE);

        // 2) Validate old password
        boolean oldOk = CryptoUtil.match(request.getOldPassword(), user.getPasswordHash());
        BusinessException.throwIf(!oldOk,
                ResponseCode.UNAUTHORIZED,
                ResponseMessageKey.INVALID_CREDENTIALS_MESSAGE);

        // 3) Update to new password
        user.setPasswordHash(CryptoUtil.encode(request.getNewPassword()));
        sysUsersService.updateById(user);

        // 4) Force logout all devices: batch deactivate sessions and revoke refresh
        // tokens by userId
        Long userId = user.getId();
        sysSessionsService.lambdaUpdate()
                .set(SysSessions::getActive, SessionStatusEnum.INACTIVE.getValue())
                .eq(SysSessions::getUserId, userId)
                .eq(SysSessions::getActive, SessionStatusEnum.ACTIVE.getValue())
                .update();
        sysRefreshTokensService.lambdaUpdate()
                .set(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.REVOKED.getValue())
                .eq(SysRefreshTokens::getUserId, userId)
                .eq(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.NOT_REVOKED.getValue())
                .update();

        StpUtil.logout();
        return true;
    }
}
