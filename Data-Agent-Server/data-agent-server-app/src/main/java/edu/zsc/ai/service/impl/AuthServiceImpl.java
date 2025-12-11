package edu.zsc.ai.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.request.LoginRequest;
import edu.zsc.ai.model.dto.request.RegisterRequest;
import edu.zsc.ai.model.dto.response.GitHubTokenResponse;
import edu.zsc.ai.model.dto.response.GitHubUserInfo;
import edu.zsc.ai.model.dto.response.GoogleTokenResponse;
import edu.zsc.ai.model.dto.response.GoogleUserInfo;
import edu.zsc.ai.model.dto.response.TokenPairResponse;
import edu.zsc.ai.model.dto.response.UserInfoResponse;
import edu.zsc.ai.model.entity.RefreshToken;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.model.entity.User;
import edu.zsc.ai.service.AuthService;
import edu.zsc.ai.service.GoogleOAuthService;
import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.service.SessionService;
import edu.zsc.ai.service.UserService;
import edu.zsc.ai.service.VerificationCodeService;
import edu.zsc.ai.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final VerificationCodeService verificationCodeService;
    private final GoogleOAuthService googleOAuthService;
    private final edu.zsc.ai.service.GitHubOAuthService gitHubOAuthService;

    @Override
    public TokenPairResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. Check if user exists
        User user = userService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Invalid email or password");
        }

        // 2. Verify password
        if (!PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Invalid email or password");
        }

        // 3. Check if email is verified (enabled for security)
        if (!Boolean.TRUE.equals(user.getVerified())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "Email not verified. Please check your email for verification code.");
        }

        // 4. Generate Access Token using Sa-Token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();

        // 5. Create Session and Refresh Token in transaction
        return createSessionAndTokens(user, accessToken, ipAddress, userAgent, email);
    }

    @Transactional
    protected TokenPairResponse createSessionAndTokens(User user, String accessToken, 
                                                      String ipAddress, String userAgent, 
                                                      String email) {
        // Hash the access token using SHA256 before storing
        String accessTokenHash = edu.zsc.ai.util.HashUtil.sha256(accessToken);
        
        // Create Session record (store AccessToken SHA256 hash)
        Session session = sessionService.createSession(user.getId(), accessTokenHash, ipAddress, userAgent);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), session.getId());

        // Return token pair (return original token, not hash)
        return TokenPairResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30 minutes (matching Sa-Token config)
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
        user.setPasswordHash(encodedPassword);
        user.setUsername(request.getUsername());
        user.setVerified(false); // Require email verification

        // Save to database
        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to register user");
        }

        // Send verification email
        try {
            verificationCodeService.sendCode(request.getEmail(), "EMAIL_VERIFICATION", "system");
            log.info("User registered successfully, verification email sent: email={}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email: email={}", request.getEmail(), e);
            // Don't fail registration if email sending fails
        }

        return true;
    }

    @Override
    public UserInfoResponse getCurrentUser(String token) {
        // 1. Get user ID from Sa-Token
        Long userId = StpUtil.getLoginIdAsLong();

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
                .avatarUrl(user.getAvatarUrl())
                .verified(user.getVerified())
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

        // 3. Generate new Access Token using Sa-Token
        StpUtil.login(user.getId());
        String newAccessToken = StpUtil.getTokenValue();

        // 4. Update session and tokens in transaction
        return updateSessionAndTokens(refreshTokenPlain, refreshToken, newAccessToken, user);
    }

    @Transactional
    protected TokenPairResponse updateSessionAndTokens(String refreshTokenPlain, RefreshToken refreshToken, 
                                                      String newAccessToken, User user) {
        // Immediately revoke old Refresh Token (one-time use)
        refreshTokenService.revoke(refreshTokenPlain);

        // Hash the new access token before storing
        String newAccessTokenHash = edu.zsc.ai.util.HashUtil.sha256(newAccessToken);
        
        // Update Session record (new Access Token hash)
        sessionService.updateRefreshTime(refreshToken.getSessionId(), newAccessTokenHash);

        // Generate new Refresh Token
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId(), refreshToken.getSessionId());

        log.info("Token refreshed successfully for user: email={}", user.getEmail());

        // Return new token pair (return original token, not hash)
        return TokenPairResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30 minutes (matching Sa-Token config)
                .build();
    }

    @Override
    @Transactional
    public boolean logout(String token) {
        try {
            // 1. Get user ID and token from Sa-Token
            Long userId = StpUtil.getLoginIdAsLong();
            String accessToken = StpUtil.getTokenValue();

            // 2. Logout from Sa-Token
            StpUtil.logout();

            // 3. Hash the token to find session
            String accessTokenHash = edu.zsc.ai.util.HashUtil.sha256(accessToken);
            Session session = sessionService.getByAccessToken(accessTokenHash);
            if (session != null) {
                sessionService.revokeSession(session.getId());
                
                // 4. Revoke all refresh tokens for this session
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid or expired verification code");
        }

        // 2. Check if user exists
        User user = userService.getByEmail(email);
        if (user == null) {
            // Auto-register user if not exists (optional behavior)
            user = autoRegisterUser(email);
        }

        // 3. Generate Access Token using Sa-Token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();

        // 5. Create Session and Refresh Token in transaction
        TokenPairResponse response = createSessionAndTokens(user, accessToken, ipAddress, userAgent, email);

        log.info("User logged in by email code: email={}", email);
        return response;
    }

    @Transactional
    protected User autoRegisterUser(String email) {
        // Create new user with email only
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]); // Use email prefix as username
        user.setPasswordHash("OAUTH_USER_NO_PASSWORD"); // Placeholder for OAuth users
        user.setVerified(true); // Email verified by code

        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create user");
        }

        log.info("Auto-registered user: email={}", email);
        return user;
    }

    @Override
    public boolean resetPassword(String email, String code, String newPassword) {
        // 1. Verify code
        boolean valid = verificationCodeService.verifyCode(email, code, "RESET_PASSWORD");
        if (!valid) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid or expired verification code");
        }

        // 2. Check if user exists
        User user = userService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // 3. Reset password (this will also invalidate all sessions and tokens)
        boolean success = userService.resetPassword(email, newPassword);

        log.info("Password reset successfully for user: email={}", email);
        return success;
    }

    @Override
    public TokenPairResponse googleLogin(String code, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // 1. Exchange authorization code for tokens
            log.debug("Exchanging Google authorization code for tokens");
            GoogleTokenResponse tokenResponse = googleOAuthService.exchangeCode(code);

            // 2. Validate and extract user info from ID token
            log.debug("Validating ID token and extracting user info");
            GoogleUserInfo googleUserInfo = googleOAuthService.validateAndExtractUserInfo(tokenResponse.getIdToken());

            // 3. Check if user exists
            User user = userService.getByEmail(googleUserInfo.getEmail());
            if (user == null) {
                // Create new user from Google info
                log.info("Creating new user from Google OAuth: email={}", googleUserInfo.getEmail());
                user = createUserFromGoogleInfo(googleUserInfo);
            } else {
                // Update existing user's profile
                log.info("Existing user logging in via Google OAuth: email={}", googleUserInfo.getEmail());
                updateUserFromGoogleInfo(user, googleUserInfo);
            }

            // 4. Generate access token using Sa-Token
            StpUtil.login(user.getId());
            String accessToken = StpUtil.getTokenValue();

            // 6. Create session and refresh token
            TokenPairResponse response = createSessionAndTokens(user, accessToken, ipAddress, userAgent, user.getEmail());

            log.info("Google OAuth login successful: email={}", user.getEmail());
            return response;

        } catch (BusinessException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Google OAuth login failed. Please try again.");
        }
    }

    /**
     * Create user from Google OAuth info
     * This is a helper method for Google OAuth login
     */
    @Transactional
    protected User createUserFromGoogleInfo(GoogleUserInfo googleUserInfo) {
        User user = new User();
        user.setEmail(googleUserInfo.getEmail());
        user.setUsername(googleUserInfo.getName() != null ? googleUserInfo.getName() : googleUserInfo.getEmail().split("@")[0]);
        user.setAvatarUrl(googleUserInfo.getPicture());
        user.setVerified(Boolean.TRUE.equals(googleUserInfo.getEmailVerified()));
        user.setPasswordHash("OAUTH_USER_NO_PASSWORD"); // Placeholder for OAuth users
        // No password for OAuth users

        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create user from Google OAuth");
        }

        log.info("Created user from Google OAuth: email={}", user.getEmail());
        return user;
    }

    /**
     * Update existing user with Google OAuth info
     * Updates avatar, name, and OAuth info if changed, but preserves password and status
     */
    @Transactional
    protected void updateUserFromGoogleInfo(User user, GoogleUserInfo googleUserInfo) {
        boolean updated = false;

        // Update avatar if changed
        if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(googleUserInfo.getPicture());
            updated = true;
        }

        // Update username if changed and user doesn't have a custom username
        if (googleUserInfo.getName() != null && !googleUserInfo.getName().equals(user.getUsername())) {
            // Only update if current username looks like it was auto-generated (email prefix)
            if (user.getUsername() != null && user.getUsername().equals(user.getEmail().split("@")[0])) {
                user.setUsername(googleUserInfo.getName());
                updated = true;
            }
        }

        // Save if any changes were made
        if (updated) {
            boolean saved = userService.updateById(user);
            if (!saved) {
                log.warn("Failed to update user profile from Google OAuth: email={}", user.getEmail());
            } else {
                log.info("Updated user profile from Google OAuth: email={}", user.getEmail());
            }
        }
    }

    @Override
    public TokenPairResponse githubLogin(String code, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            // 1. Exchange authorization code for tokens
            log.debug("Exchanging GitHub authorization code for tokens");
            GitHubTokenResponse tokenResponse = gitHubOAuthService.exchangeCode(code);

            // 2. Get user info from GitHub API
            log.debug("Fetching user info from GitHub API");
            GitHubUserInfo gitHubUserInfo = gitHubOAuthService.getUserInfo(tokenResponse.getAccessToken());

            // 3. Check if user exists
            User user = userService.getByEmail(gitHubUserInfo.getEmail());
            if (user == null) {
                // Create new user from GitHub info
                log.info("Creating new user from GitHub OAuth: email={}", gitHubUserInfo.getEmail());
                user = createUserFromGitHubInfo(gitHubUserInfo);
            } else {
                // Update existing user's profile
                log.info("Existing user logging in via GitHub OAuth: email={}", gitHubUserInfo.getEmail());
                updateUserFromGitHubInfo(user, gitHubUserInfo);
            }

            // 4. Generate access token using Sa-Token
            StpUtil.login(user.getId());
            String accessToken = StpUtil.getTokenValue();

            // 5. Create session and refresh token
            TokenPairResponse response = createSessionAndTokens(user, accessToken, ipAddress, userAgent, user.getEmail());

            log.info("GitHub OAuth login successful: email={}", user.getEmail());
            return response;

        } catch (BusinessException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("GitHub OAuth login failed", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "GitHub OAuth login failed. Please try again.");
        }
    }

    /**
     * Create user from GitHub OAuth info
     * This is a helper method for GitHub OAuth login
     */
    @Transactional
    protected User createUserFromGitHubInfo(GitHubUserInfo gitHubUserInfo) {
        User user = new User();
        user.setEmail(gitHubUserInfo.getEmail());
        user.setUsername(gitHubUserInfo.getLogin() != null ? gitHubUserInfo.getLogin() : gitHubUserInfo.getEmail().split("@")[0]);
        user.setAvatarUrl(gitHubUserInfo.getAvatarUrl());
        user.setVerified(true); // GitHub emails are verified
        user.setPasswordHash("OAUTH_USER_NO_PASSWORD"); // Placeholder for OAuth users
        // No password for OAuth users

        boolean saved = userService.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create user from GitHub OAuth");
        }

        log.info("Created user from GitHub OAuth: email={}", user.getEmail());
        return user;
    }

    /**
     * Update existing user with GitHub OAuth info
     * Updates avatar, name, and OAuth info if changed, but preserves password and status
     */
    @Transactional
    protected void updateUserFromGitHubInfo(User user, GitHubUserInfo gitHubUserInfo) {
        boolean updated = false;

        // Update avatar if changed
        if (gitHubUserInfo.getAvatarUrl() != null && !gitHubUserInfo.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(gitHubUserInfo.getAvatarUrl());
            updated = true;
        }

        // Update username if changed and user doesn't have a custom username
        if (gitHubUserInfo.getLogin() != null && !gitHubUserInfo.getLogin().equals(user.getUsername())) {
            // Only update if current username looks like it was auto-generated (email prefix)
            if (user.getUsername() != null && user.getUsername().equals(user.getEmail().split("@")[0])) {
                user.setUsername(gitHubUserInfo.getLogin());
                updated = true;
            }
        }

        // Save if any changes were made
        if (updated) {
            boolean saved = userService.updateById(user);
            if (!saved) {
                log.warn("Failed to update user profile from GitHub OAuth: email={}", user.getEmail());
            } else {
                log.info("Updated user profile from GitHub OAuth: email={}", user.getEmail());
            }
        }
    }

    @Override
    @Transactional
    public boolean verifyEmail(String email, String code) {
        // 1. Verify code
        boolean valid = verificationCodeService.verifyCode(email, code, "EMAIL_VERIFICATION");
        if (!valid) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid or expired verification code");
        }

        // 2. Get user by email
        User user = userService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // 3. Check if already verified
        if (Boolean.TRUE.equals(user.getVerified())) {
            log.info("Email already verified: email={}", email);
            return true;
        }

        // 4. Update email verified status
        user.setVerified(true);
        boolean updated = userService.updateById(user);
        
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to verify email");
        }

        log.info("Email verified successfully: email={}", email);
        return true;
    }

    @Override
    public boolean updateUserProfile(Long userId, String username, String avatar, String phone) {
        return userService.updateProfile(userId, username, avatar, phone);
    }
}
