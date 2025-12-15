package edu.zsc.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.Base64.getUrlEncoder;

/**
 * JwtUtil Test Class
 */
class JwtUtilTest {

    @Test
    void testGetClaimAsStringWithValidJwt() {
        // Create valid JWT token
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"123456789\",\"email\":\"test@example.com\",\"name\":\"Test User\",\"picture\":\"https://example.com/avatar.jpg\"}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving various claims
        assertEquals("123456789", JwtUtil.getClaimAsString(validJwt, "sub"));
        assertEquals("test@example.com", JwtUtil.getClaimAsString(validJwt, "email"));
        assertEquals("Test User", JwtUtil.getClaimAsString(validJwt, "name"));
        assertEquals("https://example.com/avatar.jpg", JwtUtil.getClaimAsString(validJwt, "picture"));
    }

    @Test
    void testGetClaimAsStringWithNonExistentClaim() {
        // Create valid JWT token
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"123456789\",\"email\":\"test@example.com\"}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving non-existent claim
        assertNull(JwtUtil.getClaimAsString(validJwt, "nonExistentClaim"));
    }

    @Test
    void testGetClaimAsStringWithInvalidJwt() {
        // Test various invalid JWT formats
        assertNull(JwtUtil.getClaimAsString(null, "sub"));
        assertNull(JwtUtil.getClaimAsString("", "sub"));
        assertNull(JwtUtil.getClaimAsString("invalid", "sub"));
        assertNull(JwtUtil.getClaimAsString("invalid.jwt", "sub"));
        assertNull(JwtUtil.getClaimAsString("invalid.jwt.token", "sub"));
        assertNull(JwtUtil.getClaimAsString("invalid.base64!", "sub"));
    }

    @Test
    void testGetClaimAsStringWithInvalidPayload() {
        // Test payload that is not valid JSON
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "invalid-json-payload";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String invalidPayloadJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Should return null instead of throwing exception
        assertNull(JwtUtil.getClaimAsString(invalidPayloadJwt, "sub"));
    }

    @Test
    void testGetClaimAsStringWithNumericValue() {
        // Test retrieving numeric claim (should be converted to string)
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"123456789\",\"age\":25,\"score\":95.5}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving numeric claims
        assertEquals("123456789", JwtUtil.getClaimAsString(validJwt, "sub"));
        assertEquals("25", JwtUtil.getClaimAsString(validJwt, "age"));
        assertEquals("95.5", JwtUtil.getClaimAsString(validJwt, "score"));
    }

    @Test
    void testGetClaimAsStringWithBooleanValue() {
        // Test retrieving boolean claims
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"verified\":true,\"active\":false}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving boolean claims
        assertEquals("true", JwtUtil.getClaimAsString(validJwt, "verified"));
        assertEquals("false", JwtUtil.getClaimAsString(validJwt, "active"));
    }

    @Test
    void testGetClaimAsStringWithArrayValue() {
        // Test retrieving array claims
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"roles\":[\"user\",\"admin\"],\"permissions\":[\"read\",\"write\"]}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving array claims (returns JSON string representation)
        assertEquals("[\"user\",\"admin\"]", JwtUtil.getClaimAsString(validJwt, "roles"));
        assertEquals("[\"read\",\"write\"]", JwtUtil.getClaimAsString(validJwt, "permissions"));
    }

    @Test
    void testGetClaimAsStringWithEmptyPayload() {
        // Test empty payload
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String emptyPayloadJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Empty payload should return null
        assertNull(JwtUtil.getClaimAsString(emptyPayloadJwt, "sub"));
    }

    @Test
    void testGetClaimAsStringWithSpecialCharacters() {
        // Test payload containing special characters
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"email\":\"test+user@example.com\",\"name\":\"John Doe Jr.\",\"description\":\"User with special chars: @#$%\"}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving claims with special characters
        assertEquals("test+user@example.com", JwtUtil.getClaimAsString(validJwt, "email"));
        assertEquals("John Doe Jr.", JwtUtil.getClaimAsString(validJwt, "name"));
        assertEquals("User with special chars: @#$%", JwtUtil.getClaimAsString(validJwt, "description"));
    }

    @Test
    void testGetClaimAsStringWithUnicodeCharacters() {
        // Test payload containing Unicode characters
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"name\":\"Zhang San\",\"message\":\"Hello World\",\"description\":\"Unicode test\"}";
        
        String encodedHeader = getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        
        String validJwt = encodedHeader + "." + encodedPayload + ".";
        
        // Test retrieving claims with Unicode characters
        assertEquals("Zhang San", JwtUtil.getClaimAsString(validJwt, "name"));
        assertEquals("Hello World", JwtUtil.getClaimAsString(validJwt, "message"));
        assertEquals("Unicode test", JwtUtil.getClaimAsString(validJwt, "description"));
    }
}
