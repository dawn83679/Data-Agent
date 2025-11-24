package edu.zsc.ai.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Google OAuth Configuration Properties
 *
 * @author Data-Agent Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {

    /**
     * Google OAuth Client ID
     */
    private String clientId;

    /**
     * Google OAuth Client Secret
     */
    private String clientSecret;

    /**
     * OAuth Redirect URI (backend callback endpoint)
     * Must match the URI registered in Google Cloud Console
     */
    private String redirectUri;

    /**
     * Frontend Redirect URI (where to redirect user after successful login)
     * This is where the user will be sent with tokens in URL parameters
     */
    private String frontendRedirectUri;

    /**
     * Check if Google OAuth is properly configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && redirectUri != null && !redirectUri.isEmpty()
                && frontendRedirectUri != null && !frontendRedirectUri.isEmpty();
    }
}
