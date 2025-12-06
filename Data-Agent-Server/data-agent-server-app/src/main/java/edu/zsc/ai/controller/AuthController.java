package edu.zsc.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.zsc.ai.config.properties.GitHubOAuthProperties;
import edu.zsc.ai.config.properties.GoogleOAuthProperties;
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
import edu.zsc.ai.service.GitHubOAuthService;
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
    private final GoogleOAuthProperties googleOAuthProperties;
    private final GitHubOAuthService gitHubOAuthService;
    private final GitHubOAuthProperties gitHubOAuthProperties;

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
    public void initiateGoogleLogin(
            @RequestParam(required = false) String fromUrl,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("Initiating Google OAuth login, fromUrl={}", fromUrl);
        
        // Use default frontend URL if fromUrl not provided
        if (fromUrl == null || fromUrl.isEmpty()) {
            fromUrl = googleOAuthProperties.getFrontendRedirectUri();
        }
        
        // Encode fromUrl into state parameter
        String state = edu.zsc.ai.util.GoogleOAuthHelper.encodeStateWithFromUrl(fromUrl);
        
        // Store state in Redis for validation in callback
        googleOAuthService.storeState(state);
        
        String authUrl = googleOAuthService.getAuthorizationUrl(state);
        log.info("Redirecting to Google authorization URL");
        response.sendRedirect(authUrl);
    }

    /**
     * Google OAuth callback
     * Public endpoint - handles redirect from Google with authorization code
     */
    @Operation(summary = "Google OAuth Callback", description = "Handle Google OAuth callback with authorization code")
    @GetMapping("/google/callback")
    public void handleGoogleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        // Try to extract fromUrl from state, fallback to default value or Referer on failure
        String fromUrl = googleOAuthProperties.getFrontendRedirectUri();
        
        try {
            // 1. Use Helper to validate callback parameters
            edu.zsc.ai.util.GoogleOAuthHelper.OauthCallbackValidation validation =
                    edu.zsc.ai.util.GoogleOAuthHelper.validateCallbackParams(code, state, error);
            
            // 2. Parse state parameter early, extract fromUrl (for subsequent error redirect)
            if (state != null && !state.isEmpty()) {
                try {
                    fromUrl = edu.zsc.ai.util.GoogleOAuthHelper.parseStateString(state);
                    log.info("Successfully parsed state parameter, fromUrl: {}", fromUrl);
                } catch (Exception e) {
                    log.warn("Failed to parse state parameter, trying to use Referer", e);
                    String referer = httpRequest.getHeader("Referer");
                    if (referer != null && !referer.isEmpty()) {
                        fromUrl = referer;
                    }
                }
            }
            
            if (!validation.isValid()) {
                log.error("OAuth callback parameter validation failed: {}", validation.getErrorMessage());
                String errorUrl = edu.zsc.ai.util.GoogleOAuthHelper.buildErrorRedirectUrl(fromUrl,
                        validation.getErrorCode());
                response.sendRedirect(errorUrl);
                return;
            }
            
            // 3. Validate state parameter against stored value
            if (!googleOAuthService.validateState(state)) {
                log.error("Invalid or expired OAuth state parameter");
                String errorUrl = edu.zsc.ai.util.GoogleOAuthHelper.buildErrorRedirectUrl(fromUrl,
                        "invalid_state");
                response.sendRedirect(errorUrl);
                return;
            }
            
            // 4. Process login and get tokens
            TokenPairResponse tokenPair = authService.googleLogin(validation.getCode(), httpRequest);
            
            log.info("Google user ready, tokens generated");
            
            // 5. Use Helper to build success redirect URL with newly generated token
            String successUrl = edu.zsc.ai.util.GoogleOAuthHelper.buildSuccessRedirectUrl(fromUrl,
                    tokenPair.getAccessToken(),
                    tokenPair.getRefreshToken());
            
            log.info("Google OAuth login successful, redirecting to: {}", successUrl);
            response.sendRedirect(successUrl);
            
        } catch (Exception e) {
            log.error("Exception occurred while processing Google OAuth callback", e);
            // Use previously extracted fromUrl, or default value if not available
            String errorUrl = edu.zsc.ai.util.GoogleOAuthHelper.buildErrorRedirectUrl(fromUrl,
                    "callback_processing_failed");
            response.sendRedirect(errorUrl);
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
     * Initiate GitHub OAuth login
     * Public endpoint - redirects user to GitHub authorization page
     */
    @Operation(summary = "Initiate GitHub OAuth", description = "Redirect to GitHub authorization page")
    @GetMapping("/github/login")
    public void initiateGitHubLogin(
            @RequestParam(required = false) String fromUrl,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("Initiating GitHub OAuth login, fromUrl={}", fromUrl);
        
        try {
            // Check if GitHub OAuth is configured
            if (!gitHubOAuthProperties.isConfigured()) {
                log.error("GitHub OAuth is not configured");
                String errorUrl = gitHubOAuthProperties.getFrontendRedirectUri() + "?error=github_not_configured";
                response.sendRedirect(errorUrl);
                return;
            }
            
            // Use default frontend URL if fromUrl not provided
            if (fromUrl == null || fromUrl.isEmpty()) {
                fromUrl = gitHubOAuthProperties.getFrontendRedirectUri();
            }
            
            // Encode fromUrl into state parameter
            String state = edu.zsc.ai.util.GitHubOAuthHelper.encodeStateWithFromUrl(fromUrl);
            
            // Store state in Redis for validation in callback
            gitHubOAuthService.storeState(state);
            
            String authUrl = gitHubOAuthService.getAuthorizationUrl(state);
            log.info("Redirecting to GitHub authorization URL");
            response.sendRedirect(authUrl);
            
        } catch (Exception e) {
            log.error("Failed to initiate GitHub OAuth login", e);
            String errorUrl = gitHubOAuthProperties.getFrontendRedirectUri() + "?error=github_init_failed";
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * GitHub OAuth callback
     * Public endpoint - handles redirect from GitHub with authorization code
     */
    @Operation(summary = "GitHub OAuth Callback", description = "Handle GitHub OAuth callback with authorization code")
    @GetMapping("/github/callback")
    public void handleGitHubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            @RequestParam(required = false) String state,
            HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("GitHub OAuth callback received: code={}, error={}, state={}", 
            code != null, error, state != null);
        
        // Extract fromUrl from state with fallback chain
        String fromUrl = extractFromUrl(state, httpRequest, gitHubOAuthProperties.getFrontendRedirectUri());
        
        try {
            // Use GitHubOAuthHelper for parameter validation
            edu.zsc.ai.util.GitHubOAuthHelper.OauthCallbackValidation validation = 
                edu.zsc.ai.util.GitHubOAuthHelper.validateCallbackParams(code, state, error);
            
            // Early error handling
            if (!validation.isValid()) {
                log.error("OAuth callback validation failed: {}", validation.getErrorMessage());
                String errorUrl = edu.zsc.ai.util.GitHubOAuthHelper.buildErrorRedirectUrl(
                    fromUrl, validation.getErrorCode());
                response.sendRedirect(errorUrl);
                return;
            }
            
            // Validate state parameter against stored value
            if (!gitHubOAuthService.validateState(state)) {
                log.error("Invalid or expired OAuth state parameter");
                String errorUrl = edu.zsc.ai.util.GitHubOAuthHelper.buildErrorRedirectUrl(
                    fromUrl, "invalid_state");
                response.sendRedirect(errorUrl);
                return;
            }
            
            // Process login and get tokens
            TokenPairResponse tokenPair = authService.githubLogin(code, httpRequest);
            
            // Build success redirect URL using helper
            String successUrl = edu.zsc.ai.util.GitHubOAuthHelper.buildSuccessRedirectUrl(
                fromUrl, tokenPair.getAccessToken(), tokenPair.getRefreshToken());
            
            log.info("GitHub OAuth login successful, redirecting to: {}", fromUrl);
            response.sendRedirect(successUrl);
            
        } catch (Exception e) {
            log.error("GitHub OAuth login failed", e);
            String errorUrl = edu.zsc.ai.util.GitHubOAuthHelper.buildErrorRedirectUrl(
                fromUrl, "callback_processing_failed");
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * GitHub OAuth login (API version)
     * Public endpoint - exchanges GitHub authorization code for tokens
     * This endpoint is for API clients (mobile apps, etc.) that handle OAuth flow themselves
     */
    @Operation(summary = "GitHub OAuth Login API", description = "Login with GitHub OAuth authorization code (API version)")
    @PostMapping("/github/login")
    public ApiResponse<TokenPairResponse> githubLogin(@Valid @RequestBody edu.zsc.ai.model.dto.request.GitHubLoginRequest request,
                                                      HttpServletRequest httpRequest) {
        log.info("GitHub OAuth login attempt (API)");
        TokenPairResponse tokenPair = authService.githubLogin(request.getCode(), httpRequest);
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
     * Test callback endpoint for debugging OAuth flow
     * This endpoint displays the OAuth result (success or error)
     */
    @Operation(summary = "Test OAuth Callback", description = "Test endpoint to display OAuth results")
    @GetMapping("/test-callback")
    public String testCallback(
            @RequestParam(required = false) String access_token,
            @RequestParam(required = false) String refresh_token,
            @RequestParam(required = false) String error) {
        if (error != null) {
            return "<html><body><h1>OAuth Error</h1><p>" + error + "</p></body></html>";
        }
        if (access_token != null) {
            return "<html><body><h1>OAuth Success!</h1>" +
                   "<p>Access Token: " + access_token.substring(0, Math.min(20, access_token.length())) + "...</p>" +
                   "<p>Refresh Token: " + (refresh_token != null ? refresh_token.substring(0, Math.min(20, refresh_token.length())) + "..." : "N/A") + "</p>" +
                   "</body></html>";
        }
        return "<html><body><h1>OAuth Callback</h1><p>No data received</p></body></html>";
    }

    /**
     * Extract fromUrl from state parameter with fallback chain
     */
    private String extractFromUrl(String state, HttpServletRequest request, String defaultUrl) {
        // Try to parse from state parameter
        if (state != null && !state.isEmpty()) {
            try {
                String fromUrl = edu.zsc.ai.util.GoogleOAuthHelper.parseStateString(state);
                if (fromUrl != null && !fromUrl.isEmpty()) {
                    log.debug("Extracted fromUrl from state: {}", fromUrl);
                    return fromUrl;
                }
            } catch (Exception e) {
                log.warn("Failed to parse fromUrl from state, using fallback", e);
            }
        }
        
        // Fallback to Referer header
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            log.debug("Using Referer header as fromUrl: {}", referer);
            return referer;
        }
        
        // Final fallback to default URL
        log.debug("Using default URL as fromUrl: {}", defaultUrl);
        return defaultUrl;
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
