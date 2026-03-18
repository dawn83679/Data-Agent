package edu.zsc.ai.domain.service.oauth;

import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public interface OAuthStrategy {
    /**
     * Get provider name (used for strategy routing)
     *
     * @return provider name (e.g., "google", "github")
     */
    String getProviderName();

    /**
     * Generate authorization redirect URL
     *
     * @param state Pass-through parameter (usually fromUrl)
     * @return Authorization URL
     */
    String getAuthorizationUrl(String state);

    /**
     * Process callback and get user info
     *
     * @param code Authorization code
     * @return Standardized user info
     */
    OAuthUserInfo getUserInfo(String code);

    /**
     * Generate nonce value (used to prevent CSRF attacks)
     *
     * @return random nonce string
     */
    default String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Extract fromUrl from state parameter
     */
    default String extractFromUrlFromState(String state) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(state);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            // State format: fromUrl.timestamp
            int dotIndex = decoded.lastIndexOf('.');
            if (dotIndex > 0) {
                return decoded.substring(0, dotIndex);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build success redirect URL with tokens
     */
    default String buildSuccessRedirectUrl(String fromUrl, String accessToken, String refreshToken) {
        try {
            if (StringUtils.isBlank(fromUrl)) {
                return null;
            }

            StringBuilder url = new StringBuilder(fromUrl);
            String separator = fromUrl.contains("?") ? "&" : "?";

            url.append(separator).append("access_token").append("=")
                    .append(encodeParameter(accessToken));
            url.append("&").append("refresh_token").append("=")
                    .append(encodeParameter(refreshToken));

            return url.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build error redirect URL
     */
    default String buildErrorRedirectUrl(String fromUrl, String error) {
        try {
            if (StringUtils.isBlank(fromUrl)) {
                return null;
            }

            StringBuilder url = new StringBuilder(fromUrl);
            String separator = fromUrl.contains("?") ? "&" : "?";

            url.append(separator).append("loginError").append("=")
                    .append(encodeParameter(error));
            url.append("&").append("loginSuccess").append("=false");

            return url.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate callback parameters
     */
    default boolean validateCallbackParams(String code, String state, String error) {
        // If error is present, validation fails
        if (StringUtils.isNotBlank(error)) {
            return false;
        }

        // Code must be present
        if (StringUtils.isBlank(code)) {
            return false;
        }

        return true;
    }

    /**
     * URL encode parameter
     */
    default String encodeParameter(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
