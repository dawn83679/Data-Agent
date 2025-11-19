package edu.zsc.ai.service.impl;

import edu.zsc.ai.common.ErrorCode;
import edu.zsc.ai.event.LoginEvent;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.service.LoginAttemptService;
import edu.zsc.ai.model.dto.request.LoginRequest;
import org.springframework.context.ApplicationEventPublisher;
import edu.zsc.ai.model.dto.request.RegisterRequest;
import edu.zsc.ai.model.dto.response.TokenPairResponse;
import edu.zsc.ai.model.dto.response.UserInfoResponse;
import edu.zsc.ai.model.entity.RefreshToken;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.AuthService;
import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.service.SessionService;
import edu.zsc.ai.service.UserService;
import edu.zsc.ai.service.VerificationCodeService;
import edu.zsc.ai.util.HashUtil;
import edu.zsc.ai.util.JwtUtil;
import edu.zsc.ai.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service Implementation
 *
 * @author Data-Agent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationCodeService verificationCodeService;

    @Override
    public TokenPairResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String ipAddress = getClientIp(httpRequest);

        // 1. Check if email or IP is blocked
        if (loginAttemptService.isBlocked(email)) {
            long remainingSeconds = loginAttemptService.getBlockTimeRemaining(email);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED_ERROR, 
                String.format("Too many failed login attempts. Account locked for %d seconds", remainingSeconds));
        }

        if (loginAttemptService.isBlocked(ipAddress)) {
            long remainingSeconds = loginAttemptService.getBlockTimeRemaining(ipAddress);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED_ERROR, 
                String.format("Too many failed login attempts from this IP. Blocked for %d seconds", remainingSeconds));
        }

        String userAgent = httpRequest.getHeader("User-Agent");

        // 2. Check if user exists
        User user = userService.getByEmail(email);
        if (user == null) {
            // Record failed attempt
            loginAttemptService.loginFailed(email);
            loginAttemptService.loginFailed(ipAddress);
            
            // Publish failure event asynchronously
            eventPublisher.publishEvent(LoginEvent.failure(this, email, ipAddress, userAgent, "User not found"));
            
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Invalid email or password");
        }

        // 3. Verify password
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            // Record failed attempt
            loginAttemptService.loginFailed(email);
            loginAttemptService.loginFailed(ipAddress);
            
            // Publish failure event asynchronously
            eventPublisher.publishEvent(LoginEvent.failure(this, email, ipAddress, userAgent, "Invalid password"));
            
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Invalid email or password");
        }

        // 3. Check if email is verified (optional, can be enabled later)
        // if (!user.getEmailVerified()) {
        //     throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "Email not verified");
        // }

        // 4. Check account status
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "Account is disabled");
        }

        // 5. Login successful - clear failed attempts
        loginAttemptService.loginSucceeded(email);
        loginAttemptService.loginSucceeded(ipAddress);

        // 6. Generate Access Token (JWT) - outside transaction
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String accessTokenHash = HashUtil.sha256(accessToken);

        // 7. Create Session and Refresh Token in transaction
        return createSessionAndTokens(user, accessTokenHash, ipAddress, userAgent, email, accessToken);
    }

    @Transactional
    protected TokenPairResponse createSessionAndTokens(User user, String accessTokenHash, 
                                                      String ipAddress, String userAgent, 
                                                      String email, String accessToken) {
        // Create Session record
        Session session = sessionService.createSession(user.getId(), accessTokenHash, ipAddress, userAgent);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), session.getId());

        // Publish success event asynchronously
        eventPublisher.publishEvent(LoginEvent.success(this, email, ipAddress, userAgent));

        // Return token pair
        return TokenPairResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour
                .build();
    }

    @Override
    public boolean register(RegisterRequest request) {
        // 1. Check if email already exists
        if (userService.emailExists(request.getEmail())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email already registered");
        }

        // 2. Encode password outside transaction
        String encodedPassword = PasswordUtil.encode(request.getPassword());

        // 3. Save user in transaction
        return saveNewUser(request, encodedPassword);
    }

    @Transactional
    protected boolean saveNewUser(RegisterRequest request, String encodedPassword) {
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        user.setUsername(request.getUsername());
        user.setEmailVerified(true); // Auto-verify for MVP
        user.setStatus(0); // Normal status

        // Save to database
        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to register user");
        }

        log.info("User registered successfully: email={}", user.getEmail());
        return true;
    }

    @Override
    public UserInfoResponse getCurrentUser(String token) {
        // 1. Parse token to get user ID
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 2. Get user from database
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // 3. Return user info
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .build();
    }

    @Override
    public TokenPairResponse refreshToken(String refreshTokenPlain) {
        // 1. Verify Refresh Token (check hash, expiration, revocation status)
        RefreshToken refreshToken = refreshTokenService.verifyAndGet(refreshTokenPlain);

        // 2. Get user information
        User user = userService.getById(refreshToken.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // 3. Generate new Access Token (JWT) - outside transaction
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String newAccessTokenHash = HashUtil.sha256(newAccessToken);

        // 4. Update session and tokens in transaction
        return updateSessionAndTokens(refreshTokenPlain, refreshToken, newAccessTokenHash, user, newAccessToken);
    }

    @Transactional
    protected TokenPairResponse updateSessionAndTokens(String refreshTokenPlain, RefreshToken refreshToken, 
                                                      String newAccessTokenHash, User user, String newAccessToken) {
        // Immediately revoke old Refresh Token (one-time use)
        refreshTokenService.revoke(refreshTokenPlain);

        // Update Session record (new Access Token hash)
        sessionService.updateRefreshTime(refreshToken.getSessionId(), newAccessTokenHash);

        // Generate new Refresh Token
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId(), refreshToken.getSessionId());

        log.info("Token refreshed successfully for user: email={}", user.getEmail());

        // Return new token pair
        return TokenPairResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour
                .build();
    }

    @Override
    @Transactional
    public boolean logout(String token) {
        try {
            // 1. Parse token to get user ID
            Long userId = jwtUtil.getUserIdFromToken(token);
            String tokenHash = HashUtil.sha256(token);

            // 2. Find and revoke session
            Session session = sessionService.getByAccessTokenHash(tokenHash);
            if (session != null) {
                sessionService.revokeSession(session.getId());
                
                // 3. Revoke all refresh tokens for this session
                refreshTokenService.revokeAllUserTokens(userId);
            }

            log.info("User logged out successfully: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("Logout failed", e);
            return false;
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public TokenPairResponse loginByEmailCode(String email, String code, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. Verify code
        boolean valid = verificationCodeService.verifyCode(email, code, "LOGIN");
        if (!valid) {
            // Publish failure event
            eventPublisher.publishEvent(LoginEvent.failure(this, email, ipAddress, userAgent, "Invalid verification code"));
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid or expired verification code");
        }

        // 2. Check if user exists
        User user = userService.getByEmail(email);
        if (user == null) {
            // Auto-register user if not exists (optional behavior)
            user = autoRegisterUser(email);
        }

        // 3. Check account status
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "Account is disabled");
        }

        // 4. Generate Access Token (JWT) - outside transaction
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String accessTokenHash = HashUtil.sha256(accessToken);

        // 5. Create Session and Refresh Token in transaction
        TokenPairResponse response = createSessionAndTokens(user, accessTokenHash, ipAddress, userAgent, email, accessToken);

        log.info("User logged in by email code: email={}", email);
        return response;
    }

    @Transactional
    protected User autoRegisterUser(String email) {
        // Create new user with email only
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]); // Use email prefix as username
        user.setEmailVerified(true); // Email verified by code
        user.setStatus(0); // Normal status
        // No password for email code login users

        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create user");
        }

        log.info("Auto-registered user: email={}", email);
        return user;
    }
}
