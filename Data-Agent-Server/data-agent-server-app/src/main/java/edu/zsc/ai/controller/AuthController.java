package edu.zsc.ai.controller;

import edu.zsc.ai.model.dto.request.EmailCodeLoginRequest;
import edu.zsc.ai.model.dto.request.LoginRequest;
import edu.zsc.ai.model.dto.request.RefreshTokenRequest;
import edu.zsc.ai.model.dto.request.RegisterRequest;
import edu.zsc.ai.model.dto.request.SendVerificationCodeRequest;
import edu.zsc.ai.model.dto.response.ApiResponse;
import edu.zsc.ai.model.dto.response.TokenPairResponse;
import edu.zsc.ai.service.AuthService;
import edu.zsc.ai.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles authentication-related operations: login, register, token refresh, logout
 * Most endpoints are public (no authentication required)
 *
 * @author Data-Agent Team
 */
@Tag(name = "Authentication", description = "Authentication APIs - Token, Session, Credential management")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;

    /**
     * User login
     * Public endpoint - no authentication required
     */
    @Operation(summary = "User Login", description = "Login with email and password to obtain access token")
    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        log.info("User login attempt: email={}", request.getEmail());
        TokenPairResponse tokenPair = authService.login(request, httpRequest);
        return ApiResponse.success(tokenPair);
    }

    /**
     * Send verification code
     * Public endpoint - no authentication required
     */
    @Operation(summary = "Send Verification Code", description = "Send verification code to email")
    @PostMapping("/send-code")
    public ApiResponse<Boolean> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request,
                                                     HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        log.info("Send verification code: email={}, type={}", request.getEmail(), request.getCodeType());
        boolean result = verificationCodeService.sendCode(request.getEmail(), request.getCodeType(), ipAddress);
        return ApiResponse.success(result);
    }

    /**
     * Login by email verification code
     * Public endpoint - no authentication required
     */
    @Operation(summary = "Login by Email Code", description = "Login with email verification code")
    @PostMapping("/login/email-code")
    public ApiResponse<TokenPairResponse> loginByEmailCode(@Valid @RequestBody EmailCodeLoginRequest request,
                                                           HttpServletRequest httpRequest) {
        log.info("Email code login attempt: email={}", request.getEmail());
        TokenPairResponse tokenPair = authService.loginByEmailCode(request.getEmail(), request.getCode(), httpRequest);
        return ApiResponse.success(tokenPair);
    }

    /**
     * User registration
     * Public endpoint - no authentication required
     */
    @Operation(summary = "User Registration", description = "Register a new user account")
    @PostMapping("/register")
    public ApiResponse<Boolean> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration attempt: email={}", request.getEmail());
        boolean result = authService.register(request);
        return ApiResponse.success(result);
    }

    /**
     * Refresh access token
     * Public endpoint - requires valid refresh token
     */
    @Operation(summary = "Refresh Token", description = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh attempt");
        TokenPairResponse tokenPair = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(tokenPair);
    }

    /**
     * User logout
     * Requires authentication - revokes current session and tokens
     */
    @Operation(summary = "User Logout", description = "Logout current user and revoke session")
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        log.debug("User logout attempt");
        boolean result = authService.logout(token);
        return ApiResponse.success(result);
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
