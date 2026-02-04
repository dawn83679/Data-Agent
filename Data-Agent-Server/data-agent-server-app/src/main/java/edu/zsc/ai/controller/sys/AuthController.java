package edu.zsc.ai.controller.sys;

import edu.zsc.ai.domain.model.dto.request.sys.LoginRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RefreshTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RegisterRequest;
import edu.zsc.ai.domain.model.dto.request.sys.ResetPasswordRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.sys.TokenPairResponse;
import edu.zsc.ai.domain.service.sys.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@RequestBody @Validated LoginRequest request) {
        TokenPairResponse data = authService.loginByEmail(request);
        return ApiResponse.success(data);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@RequestBody @Validated RefreshTokenRequest request) {
        TokenPairResponse data = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(data);
    }

    @PostMapping("/register")
    public ApiResponse<Boolean> register(@RequestBody @Validated RegisterRequest request) {
        Boolean data = authService.register(request);
        return ApiResponse.success(data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Validated ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }

}
