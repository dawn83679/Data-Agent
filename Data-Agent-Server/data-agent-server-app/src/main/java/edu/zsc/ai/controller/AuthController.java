package edu.zsc.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import edu.zsc.ai.model.dto.request.EmailCodeLoginRequest;
import edu.zsc.ai.model.dto.request.GoogleLoginRequest;
import edu.zsc.ai.model.dto.request.LoginRequest;
import edu.zsc.ai.model.dto.request.RefreshTokenRequest;
import edu.zsc.ai.model.dto.request.RegisterRequest;
import edu.zsc.ai.model.dto.request.ResetPasswordRequest;
import edu.zsc.ai.model.dto.request.SendVerificationCodeRequest;
import edu.zsc.ai.model.dto.response.TokenPairResponse;
import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.service.AuthService;
import edu.zsc.ai.service.GoogleOAuthService;
import edu.zsc.ai.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final GoogleOAuthService googleOAuthService;
    private final edu.zsc.ai.config.properties.GoogleOAuthProperties googleOAuthProperties;

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
     * Reset password
     * Public endpoint - requires verification code
     */
    @Operation(summary = "Reset Password", description = "Reset password with verification code")
    @PostMapping("/reset-password")
    public ApiResponse<Boolean> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt: email={}", request.getEmail());
        boolean result = authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ApiResponse.success(result);
    }

    /**
     * Initiate Google OAuth login
     * Public endpoint - redirects user to Google authorization page
     */
    @Operation(summary = "Initiate Google OAuth", description = "Redirect to Google authorization page")
    @GetMapping("/google/login")
    public RedirectView initiateGoogleLogin() {
        log.info("Initiating Google OAuth login");
        // Generate secure random state for CSRF protection
        String state = ((edu.zsc.ai.service.impl.GoogleOAuthServiceImpl) googleOAuthService).generateState();
        // Store state in Redis for validation in callback
        googleOAuthService.storeState(state);
        
        String authUrl = googleOAuthService.getAuthorizationUrl(state);
        return new RedirectView(authUrl);
    }

    /**
     * Google OAuth callback
     * Public endpoint - handles redirect from Google with authorization code
     */
    @Operation(summary = "Google OAuth Callback", description = "Handle Google OAuth callback with authorization code")
    @GetMapping("/google/callback")
    public RedirectView handleGoogleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest httpRequest) {
        log.info("Google OAuth callback received");
        
        try {
            // Validate state parameter against stored value
            if (!googleOAuthService.validateState(state)) {
                log.error("Invalid or expired OAuth state parameter");
                String errorUrl = buildFrontendErrorUrl("Invalid or expired OAuth state. Please try again.");
                return new RedirectView(errorUrl);
            }
            
            // Process login and get tokens
            TokenPairResponse tokenPair = authService.googleLogin(code, httpRequest);
            
            // Redirect to frontend with tokens
            String frontendUrl = buildFrontendRedirectUrl(tokenPair);
            log.info("Google OAuth login successful, redirecting to frontend");
            return new RedirectView(frontendUrl);
            
        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            // Redirect to frontend error page
            String errorUrl = buildFrontendErrorUrl(e.getMessage());
            return new RedirectView(errorUrl);
        }
    }

    /**
     * Google OAuth login (API version)
     * Public endpoint - exchanges Google authorization code for tokens
     * This endpoint is for API clients (mobile apps, etc.) that handle OAuth flow themselves
     */
    @Operation(summary = "Google OAuth Login API", description = "Login with Google OAuth authorization code (API version)")
    @PostMapping("/google/login")
    public ApiResponse<TokenPairResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                      HttpServletRequest httpRequest) {
        log.info("Google OAuth login attempt (API)");
        TokenPairResponse tokenPair = authService.googleLogin(request.getCode(), httpRequest);
        return ApiResponse.success(tokenPair);
    }

    /**
     * Verify email with verification code
     * Public endpoint - verifies user email address
     */
    @Operation(summary = "Verify Email", description = "Verify email address with verification code")
    @PostMapping("/verify-email")
    public ApiResponse<Boolean> verifyEmail(
            @RequestParam String email,
            @RequestParam String code) {
        log.info("Email verification attempt: email={}", email);
        boolean result = authService.verifyEmail(email, code);
        return ApiResponse.success(result);
    }

    /**
     * Get login history
     * Requires authentication
     */
    @Operation(summary = "Get Login History", description = "Get current user's login history")
    @PostMapping("/login-history")
    public ApiResponse<java.util.List<edu.zsc.ai.model.entity.LoginLog>> getLoginHistory() {
        log.debug("Get login history request");
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        java.util.List<edu.zsc.ai.model.entity.LoginLog> history = 
            authService.getLoginHistory(userId, 20);
        return ApiResponse.success(history);
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

    /**
     * Build frontend redirect URL with tokens
     */
    private String buildFrontendRedirectUrl(TokenPairResponse tokenPair) {
        String frontendUrl = googleOAuthProperties.getFrontendRedirectUri();
        
        try {
            return frontendUrl + "?" +
                "access_token=" + java.net.URLEncoder.encode(tokenPair.getAccessToken(), "UTF-8") +
                "&refresh_token=" + java.net.URLEncoder.encode(tokenPair.getRefreshToken(), "UTF-8") +
                "&token_type=" + java.net.URLEncoder.encode(tokenPair.getTokenType(), "UTF-8") +
                "&expires_in=" + tokenPair.getExpiresIn();
        } catch (Exception e) {
            log.error("Failed to encode tokens in URL", e);
            return frontendUrl + "?error=encoding_failed";
        }
    }

    /**
     * Build frontend error URL
     */
    private String buildFrontendErrorUrl(String errorMessage) {
        String frontendUrl = googleOAuthProperties.getFrontendRedirectUri();
        try {
            return frontendUrl + "?error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8");
        } catch (Exception e) {
            log.error("Failed to encode error message", e);
            return frontendUrl + "?error=unknown";
        }
    }
}
