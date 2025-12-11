package edu.zsc.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import edu.zsc.ai.config.properties.GoogleOAuthProperties;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.GoogleUserInfo;

/**
 * Google OAuth Service Implementation Test
 *
 * @author Data-Agent Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleOAuthServiceImplTest {

    @Mock
    private GoogleOAuthProperties googleOAuthProperties;

    @InjectMocks
    private GoogleOAuthServiceImpl googleOAuthService;

    @BeforeEach
    void setUp() {
        // Setup mock configuration
        when(googleOAuthProperties.getClientId()).thenReturn("test-client-id");
        when(googleOAuthProperties.getClientSecret()).thenReturn("test-client-secret");
        when(googleOAuthProperties.getRedirectUri()).thenReturn("http://localhost:8081/api/auth/google/callback");
        when(googleOAuthProperties.getAuthUrl()).thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        when(googleOAuthProperties.getScope()).thenReturn("openid email profile");
        when(googleOAuthProperties.isConfigured()).thenReturn(true);
    }

    @Test
    void testGetAuthorizationUrl_Success() {
        // Given
        String state = "test-state-123";

        // When
        String authUrl = googleOAuthService.getAuthorizationUrl(state);

        // Then
        assertNotNull(authUrl);
        assertTrue(authUrl.contains("accounts.google.com"));
        assertTrue(authUrl.contains("client_id=test-client-id"));
        assertTrue(authUrl.contains("redirect_uri="));
        assertTrue(authUrl.contains("response_type=code"));
        assertTrue(authUrl.contains("scope="));
        assertTrue(authUrl.contains("state=test-state-123"));
    }

    @Test
    void testGetAuthorizationUrl_NotConfigured() {
        // Given
        when(googleOAuthProperties.isConfigured()).thenReturn(false);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            googleOAuthService.getAuthorizationUrl("test-state");
        });
    }

    @Test
    void testGenerateState() {
        // When
        String state1 = googleOAuthService.generateState();
        String state2 = googleOAuthService.generateState();

        // Then
        assertNotNull(state1);
        assertNotNull(state2);
        assertNotEquals(state1, state2); // Should generate different states
        assertTrue(state1.length() > 20); // Should be reasonably long
    }

    @Test
    void testValidateAndExtractUserInfo_ValidToken() {
        // Given - A sample JWT token (header.payload.signature)
        // This is a mock token with valid structure but fake signature
        String header = encodeBase64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encodeBase64Url("{" +
            "\"iss\":\"https://accounts.google.com\"," +
            "\"aud\":\"test-client-id\"," +
            "\"sub\":\"123456789\"," +
            "\"email\":\"test@example.com\"," +
            "\"email_verified\":true," +
            "\"name\":\"Test User\"," +
            "\"picture\":\"https://example.com/photo.jpg\"," +
            "\"given_name\":\"Test\"," +
            "\"family_name\":\"User\"," +
            "\"locale\":\"en\"," +
            "\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) +
            "}");
        String signature = "fake-signature";
        String idToken = header + "." + payload + "." + signature;

        // When
        GoogleUserInfo userInfo = googleOAuthService.validateAndExtractUserInfo(idToken);

        // Then
        assertNotNull(userInfo);
        assertEquals("123456789", userInfo.getGoogleId());
        assertEquals("test@example.com", userInfo.getEmail());
        assertTrue(userInfo.getEmailVerified());
        assertEquals("Test User", userInfo.getName());
        assertEquals("https://example.com/photo.jpg", userInfo.getPicture());
        assertEquals("Test", userInfo.getGivenName());
        assertEquals("User", userInfo.getFamilyName());
        assertEquals("en", userInfo.getLocale());
    }

    @Test
    void testValidateAndExtractUserInfo_InvalidIssuer() {
        // Given - Token with invalid issuer
        String header = encodeBase64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encodeBase64Url("{" +
            "\"iss\":\"https://evil.com\"," +
            "\"aud\":\"test-client-id\"," +
            "\"sub\":\"123456789\"," +
            "\"email\":\"test@example.com\"," +
            "\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) +
            "}");
        String signature = "fake-signature";
        String idToken = header + "." + payload + "." + signature;

        // When & Then
        assertThrows(BusinessException.class, () -> {
            googleOAuthService.validateAndExtractUserInfo(idToken);
        });
    }

    @Test
    void testValidateAndExtractUserInfo_InvalidAudience() {
        // Given - Token with invalid audience
        String header = encodeBase64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encodeBase64Url("{" +
            "\"iss\":\"https://accounts.google.com\"," +
            "\"aud\":\"wrong-client-id\"," +
            "\"sub\":\"123456789\"," +
            "\"email\":\"test@example.com\"," +
            "\"exp\":" + (System.currentTimeMillis() / 1000 + 3600) +
            "}");
        String signature = "fake-signature";
        String idToken = header + "." + payload + "." + signature;

        // When & Then
        assertThrows(BusinessException.class, () -> {
            googleOAuthService.validateAndExtractUserInfo(idToken);
        });
    }

    @Test
    void testValidateAndExtractUserInfo_ExpiredToken() {
        // Given - Expired token
        String header = encodeBase64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encodeBase64Url("{" +
            "\"iss\":\"https://accounts.google.com\"," +
            "\"aud\":\"test-client-id\"," +
            "\"sub\":\"123456789\"," +
            "\"email\":\"test@example.com\"," +
            "\"exp\":" + (System.currentTimeMillis() / 1000 - 3600) + // Expired 1 hour ago
            "}");
        String signature = "fake-signature";
        String idToken = header + "." + payload + "." + signature;

        // When & Then
        assertThrows(BusinessException.class, () -> {
            googleOAuthService.validateAndExtractUserInfo(idToken);
        });
    }

    @Test
    void testValidateAndExtractUserInfo_InvalidFormat() {
        // Given - Invalid token format
        String idToken = "invalid.token";

        // When & Then
        assertThrows(BusinessException.class, () -> {
            googleOAuthService.validateAndExtractUserInfo(idToken);
        });
    }

    /**
     * Helper method to encode string to Base64 URL format
     */
    private String encodeBase64Url(String input) {
        return java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
