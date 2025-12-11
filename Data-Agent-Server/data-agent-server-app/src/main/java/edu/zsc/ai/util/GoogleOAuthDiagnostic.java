package edu.zsc.ai.util;

import org.springframework.stereotype.Component;

import edu.zsc.ai.config.properties.GoogleOAuthProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth Configuration Diagnostic Tool
 * Helps identify configuration issues
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthDiagnostic {

    private final GoogleOAuthProperties properties;

    @PostConstruct
    public void diagnose() {
        log.info("=== Google OAuth Configuration Diagnostic ===");
        log.info("Client ID: {}", maskSensitive(properties.getClientId()));
        log.info("Client Secret: {}", maskSensitive(properties.getClientSecret()));
        log.info("Redirect URI: {}", properties.getRedirectUri());
        log.info("Frontend Redirect URI: {}", properties.getFrontendRedirectUri());
        log.info("Auth URL: {}", properties.getAuthUrl());
        log.info("Token URL: {}", properties.getTokenUrl());
        log.info("Scope: {}", properties.getScope());
        
        // Check for common issues
        if (properties.getClientId() == null || properties.getClientId().isEmpty()) {
            log.error("❌ Client ID is not configured!");
        } else if (properties.getClientId().startsWith("${")) {
            log.error("❌ Client ID contains placeholder: {}", properties.getClientId());
        } else {
            log.info("✓ Client ID is configured");
        }
        
        if (properties.getClientSecret() == null || properties.getClientSecret().isEmpty()) {
            log.error("❌ Client Secret is not configured!");
        } else if (properties.getClientSecret().startsWith("${")) {
            log.error("❌ Client Secret contains placeholder: {}", properties.getClientSecret());
        } else {
            log.info("✓ Client Secret is configured");
        }
        
        if (properties.getRedirectUri() == null || properties.getRedirectUri().isEmpty()) {
            log.error("❌ Redirect URI is not configured!");
        } else {
            log.info("✓ Redirect URI is configured");
            log.warn("⚠ Make sure this EXACT URI is added in Google Cloud Console:");
            log.warn("   {}", properties.getRedirectUri());
        }
        
        log.info("===========================================");
    }

    private String maskSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return "[NOT SET]";
        }
        if (value.startsWith("${")) {
            return value; // Show placeholder as-is
        }
        if (value.length() <= 10) {
            return "***";
        }
        return value.substring(0, 10) + "..." + value.substring(value.length() - 4);
    }
}
