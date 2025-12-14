package edu.zsc.ai.domain.service.sys;

import edu.zsc.ai.domain.model.dto.request.sys.LoginRequest;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;

public interface AuthService {
    /**
     * Login by email
     */
    TokenPairResponse loginByEmail(LoginRequest request);

    /**
     * Refresh token
     */
    TokenPairResponse refreshToken(String refreshTokenPlain);

    /**
     * Register
     */
    Boolean register(edu.zsc.ai.domain.model.dto.request.sys.RegisterRequest request);

    /**
     * Logout
     */
    Boolean logout();

    /**
     * Reset password
     */
    Boolean resetPassword(edu.zsc.ai.domain.model.dto.request.sys.ResetPasswordRequest request);
}
