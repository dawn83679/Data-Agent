package edu.zsc.ai.domain.service.sys;

import edu.zsc.ai.domain.model.dto.request.sys.LoginRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RegisterRequest;
import edu.zsc.ai.domain.model.dto.request.sys.ResetPasswordRequest;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;
import edu.zsc.ai.domain.model.dto.response.sys.UserResponse;

public interface AuthService {
    TokenPairResponse loginByEmail(LoginRequest request);

    TokenPairResponse refreshToken(String refreshTokenPlain);

    Boolean register(RegisterRequest request);

    Boolean logout();

    Boolean resetPassword(ResetPasswordRequest request);

    UserResponse getCurrentUser();
}
