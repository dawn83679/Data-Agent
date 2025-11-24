package edu.zsc.ai.service;

import edu.zsc.ai.model.dto.request.LoginRequest;
import edu.zsc.ai.model.dto.request.RegisterRequest;
import edu.zsc.ai.model.dto.response.TokenPairResponse;
import edu.zsc.ai.model.dto.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication Service
 *
 * @author Data-Agent Team
 */
public interface AuthService {

    /**
     * User login by email and password
     */
    TokenPairResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * User registration
     */
    boolean register(RegisterRequest request);

    /**
     * Refresh access token
     */
    TokenPairResponse refreshToken(String refreshTokenPlain);

    /**
     * Get current user info
     */
    UserInfoResponse getCurrentUser(String token);

    /**
     * Logout
     */
    boolean logout(String token);

    /**
     * User login by email verification code
     */
    TokenPairResponse loginByEmailCode(String email, String code, HttpServletRequest httpRequest);

    /**
     * Reset password with verification code
     * This will invalidate all sessions and refresh tokens
     *
     * @param email user email
     * @param code verification code
     * @param newPassword new password
     * @return true if reset successful
     */
    boolean resetPassword(String email, String code, String newPassword);

    /**
     * Google OAuth login
     * Exchange authorization code for user info and create/login user
     *
     * @param code Google OAuth authorization code
     * @param httpRequest HTTP request for IP and user agent
     * @return token pair
     */
    TokenPairResponse googleLogin(String code, HttpServletRequest httpRequest);

    /**
     * Verify email with verification code
     *
     * @param email user email
     * @param code verification code
     * @return true if verification successful
     */
    boolean verifyEmail(String email, String code);

    /**
     * Get user login history
     *
     * @param userId user ID
     * @param limit maximum number of records
     * @return login history list
     */
    java.util.List<edu.zsc.ai.model.entity.LoginLog> getLoginHistory(Long userId, int limit);

    /**
     * Update user profile
     *
     * @param userId user ID
     * @param username new username (optional)
     * @param avatar new avatar URL (optional)
     * @param phone new phone number (optional)
     * @return true if update successful
     */
    boolean updateUserProfile(Long userId, String username, String avatar, String phone);
}
