package edu.zsc.ai.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import edu.zsc.ai.config.properties.GitHubOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub OAuth Configuration Validator
 * Validates OAuth configuration at application startup
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubOAuthConfigValidator {

    private final GitHubOAuthProperties gitHubOAuthProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        if (!gitHubOAuthProperties.isConfigured()) {
            log.warn("=".repeat(80));
            log.warn("GitHub OAuth is NOT configured!");
            log.warn("GitHub login endpoints will be disabled.");
            log.warn("To enable GitHub OAuth login, please configure the following properties:");
            log.warn("  - github.oauth.client-id");
            log.warn("  - github.oauth.client-secret");
            log.warn("  - github.oauth.redirect-uri");
            log.warn("  - github.oauth.frontend-redirect-uri");
            log.warn("You can set these in application.yml or as environment variables:");
            log.warn("  - GITHUB_CLIENT_ID");
            log.warn("  - GITHUB_CLIENT_SECRET");
            log.warn("  - GITHUB_REDIRECT_URI");
            log.warn("  - FRONTEND_REDIRECT_URI");
            log.warn("=".repeat(80));
        } else {
            log.info("GitHub OAuth is properly configured");
            log.info("  - Client ID: {}...", gitHubOAuthProperties.getClientId().substring(0, Math.min(20, gitHubOAuthProperties.getClientId().length())));
            log.info("  - Redirect URI: {}", gitHubOAuthProperties.getRedirectUri());
            log.info("  - Frontend Redirect URI: {}", gitHubOAuthProperties.getFrontendRedirectUri());
        }
    }
}
