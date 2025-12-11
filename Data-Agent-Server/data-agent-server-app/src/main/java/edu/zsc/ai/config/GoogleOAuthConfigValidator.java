package edu.zsc.ai.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import edu.zsc.ai.config.properties.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth Configuration Validator
 * Validates OAuth configuration at application startup
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthConfigValidator {

    private final GoogleOAuthProperties googleOAuthProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        if (!googleOAuthProperties.isConfigured()) {
            log.warn("=".repeat(80));
            log.warn("Google OAuth is NOT configured!");
            log.warn("Google login endpoints will be disabled.");
            log.warn("To enable Google OAuth login, please configure the following properties:");
            log.warn("  - google.oauth.client-id");
            log.warn("  - google.oauth.client-secret");
            log.warn("  - google.oauth.redirect-uri");
            log.warn("  - google.oauth.frontend-redirect-uri");
            log.warn("You can set these in application.yml or as environment variables:");
            log.warn("  - GOOGLE_CLIENT_ID");
            log.warn("  - GOOGLE_CLIENT_SECRET");
            log.warn("  - GOOGLE_REDIRECT_URI");
            log.warn("  - FRONTEND_REDIRECT_URI");
            log.warn("=".repeat(80));
        } else {
            log.info("Google OAuth is properly configured");
            log.info("  - Client ID: {}...", googleOAuthProperties.getClientId().substring(0, Math.min(20, googleOAuthProperties.getClientId().length())));
            log.info("  - Redirect URI: {}", googleOAuthProperties.getRedirectUri());
            log.info("  - Frontend Redirect URI: {}", googleOAuthProperties.getFrontendRedirectUri());
        }
    }
}
