package edu.zsc.ai.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.util.StringUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub OAuth Helper
 * Utility class for GitHub OAuth operations
 *
 * @author Data-Agent Team
 */
@Slf4j
public class GitHubOAuthHelper {

    public static final String ACCESS_TOKEN = "access_token";
    
    // Error codes
    public static final String ERROR_MISSING_CODE = "missing_code";
    public static final String ERROR_MISSING_STATE = "missing_state";
    public static final String ERROR_USER_DENIED = "access_denied";
    public static final String ERROR_INVALID_REQUEST = "invalid_request";

    /**
     * OAuth Callback Validation Result
     */
    @Data
    public static class OauthCallbackValidation {
        private boolean valid;
        private String errorMessage;
        private String errorCode;
        private String code;

        public static OauthCallbackValidation success(String code) {
            OauthCallbackValidation validation = new OauthCallbackValidation();
            validation.setValid(true);
            validation.setCode(code);
            return validation;
        }

        public static OauthCallbackValidation error(String errorCode, String errorMessage) {
            OauthCallbackValidation validation = new OauthCallbackValidation();
            validation.setValid(false);
            validation.setErrorCode(errorCode);
            validation.setErrorMessage(errorMessage);
            return validation;
        }
    }

    /**
     * Validate OAuth callback parameters
     */
    public static OauthCallbackValidation validateCallbackParams(String code, String state, String error) {
        // Check if user denied access
        if (StringUtils.hasText(error)) {
            return OauthCallbackValidation.error(ERROR_USER_DENIED, 
                "User denied access or authorization failed: " + error);
        }

        // Check if code is present
        if (!StringUtils.hasText(code)) {
            return OauthCallbackValidation.error(ERROR_MISSING_CODE, 
                "Missing authorization code");
        }

        // Check if state is present
        if (!StringUtils.hasText(state)) {
            return OauthCallbackValidation.error(ERROR_MISSING_STATE, 
                "Missing state parameter");
        }

        return OauthCallbackValidation.success(code);
    }

    /**
     * Encode fromUrl into state parameter
     */
    public static String encodeStateWithFromUrl(String fromUrl) {
        try {
            // Use Base64 encoding to safely encode the fromUrl
            String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(fromUrl.getBytes(StandardCharsets.UTF_8));
            return encoded;
        } catch (Exception e) {
            log.error("Failed to encode fromUrl into state", e);
            return "";
        }
    }

    /**
     * Parse state parameter to extract fromUrl
     */
    public static String parseStateString(String state) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(state);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to parse state parameter", e);
            throw new IllegalArgumentException("Invalid state parameter");
        }
    }

    /**
     * Validate token response from GitHub
     */
    public static boolean validateTokenResponse(Map<String, Object> tokenResponse) {
        return tokenResponse != null && tokenResponse.containsKey(ACCESS_TOKEN);
    }

    /**
     * Build success redirect URL with tokens
     */
    public static String buildSuccessRedirectUrl(String fromUrl, String accessToken, String refreshToken) {
        try {
            StringBuilder url = new StringBuilder(fromUrl);
            
            // Add separator
            url.append(fromUrl.contains("?") ? "&" : "?");
            
            // Add tokens
            url.append("access_token=").append(URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
            url.append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
            url.append("&token_type=Bearer");
            
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to build success redirect URL", e);
            return fromUrl + "?error=encoding_failed";
        }
    }

    /**
     * Build error redirect URL
     */
    public static String buildErrorRedirectUrl(String fromUrl, String errorCode) {
        try {
            StringBuilder url = new StringBuilder(fromUrl);
            
            // Add separator
            url.append(fromUrl.contains("?") ? "&" : "?");
            
            // Add error
            url.append("error=").append(URLEncoder.encode(errorCode, StandardCharsets.UTF_8));
            
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to build error redirect URL", e);
            return fromUrl + "?error=unknown";
        }
    }
}
