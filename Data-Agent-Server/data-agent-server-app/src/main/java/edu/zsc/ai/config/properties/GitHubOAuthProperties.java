package edu.zsc.ai.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub OAuth Configuration Properties
 *
 * @author Data-Agent Team
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthProperties {

    /**
     * GitHub OAuth Client ID
     */
    private String clientId;

    /**
     * GitHub OAuth Client Secret
     */
    private String clientSecret;

    /**
     * OAuth Redirect URI (backend callback endpoint)
     * Must match the URI registered in GitHub OAuth App settings
     */
    private String redirectUri;

    /**
     * Frontend Redirect URI (where to redirect user after successful login)
     * This is where the user will be sent with tokens in URL parameters
     */
    private String frontendRedirectUri;

    /**
     * GitHub OAuth Authorization URL
     */
    private String authUrl = "https://github.com/login/oauth/authorize";

    /**
     * GitHub OAuth Token URL
     */
    private String tokenUrl = "https://github.com/login/oauth/access_token";

    /**
     * GitHub User API URL
     */
    private String userApiUrl = "https://api.github.com/user";

    /**
     * GitHub OAuth Scope
     */
    private String scope = "read:user user:email";

    /**
     * OAuth State Redis Key Prefix
     */
    private String statePrefix = "oauth:github:state:";

    /**
     * OAuth State Expiration Time (in minutes)
     */
    private long stateExpirationMinutes = 10;

    /**
     * RestTemplate Connection Timeout (in milliseconds)
     */
    private int connectionTimeout = 10000;

    /**
     * RestTemplate Read Timeout (in milliseconds)
     */
    private int readTimeout = 10000;

    /**
     * Proxy configuration for accessing GitHub OAuth services
     */
    private Proxy proxy = new Proxy();

    /**
     * Proxy configuration nested class
     */
    @Data
    public static class Proxy {
        /**
         * Whether to enable proxy
         */
        private boolean enabled = false;

        /**
         * Proxy host (e.g., 127.0.0.1, proxy.company.com)
         */
        private String host;

        /**
         * Proxy port (e.g., 7890, 8080, 1080)
         */
        private int port;

        /**
         * Check if proxy is properly configured
         */
        public boolean isConfigured() {
            return enabled && host != null && !host.isEmpty() && port > 0;
        }
    }

    /**
     * Check if GitHub OAuth is properly configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && redirectUri != null && !redirectUri.isEmpty()
                && frontendRedirectUri != null && !frontendRedirectUri.isEmpty();
    }

    /**
     * Log configuration status on startup
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("=== GitHub OAuth Configuration ===");
        log.info("Client ID: {}", clientId != null && !clientId.isEmpty() ? "***" + clientId.substring(Math.max(0, clientId.length() - 10)) : "NOT SET");
        log.info("Client Secret: {}", clientSecret != null && !clientSecret.isEmpty() ? "***" + clientSecret.substring(Math.max(0, clientSecret.length() - 5)) : "NOT SET");
        log.info("Redirect URI: {}", redirectUri != null && !redirectUri.isEmpty() ? redirectUri : "NOT SET");
        log.info("Frontend Redirect URI: {}", frontendRedirectUri != null && !frontendRedirectUri.isEmpty() ? frontendRedirectUri : "NOT SET");
        log.info("Auth URL: {}", authUrl);
        log.info("Token URL: {}", tokenUrl);
        log.info("User API URL: {}", userApiUrl);
        log.info("Scope: {}", scope);
        log.info("State Expiration: {} minutes", stateExpirationMinutes);
        log.info("Connection Timeout: {} ms", connectionTimeout);
        log.info("Read Timeout: {} ms", readTimeout);
        log.info("Proxy Enabled: {}", proxy.isEnabled());
        if (proxy.isEnabled()) {
            log.info("Proxy Host: {}", proxy.getHost());
            log.info("Proxy Port: {}", proxy.getPort());
        }
        log.info("Is Configured: {}", isConfigured());
        log.info("==================================");
    }
}
