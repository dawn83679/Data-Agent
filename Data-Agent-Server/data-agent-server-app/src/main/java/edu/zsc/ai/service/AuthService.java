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
}
