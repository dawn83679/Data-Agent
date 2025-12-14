package edu.zsc.ai.controller;

import edu.zsc.ai.domain.model.dto.request.sys.LoginRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RegisterRequest;
import edu.zsc.ai.domain.model.dto.request.sys.ResetPasswordRequest;
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
    public TokenPairResponse login(@RequestBody @Validated LoginRequest request) {
        return authService.loginByEmail(request);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@RequestBody @Validated String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/register")
    public Boolean register(@RequestBody @Validated RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/logout")
    public Boolean logout() {
        return authService.logout();
    }

    @PostMapping("/reset-password")
    public Boolean resetPassword(@RequestBody @Validated ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

}
