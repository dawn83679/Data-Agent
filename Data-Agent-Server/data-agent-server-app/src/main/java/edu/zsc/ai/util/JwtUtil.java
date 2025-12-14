package edu.zsc.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * JWT Utility Class
 * Used for parsing JWT tokens
 *
 * @author zgq
 * @since 2025-10-01
 */
@Slf4j
public class JwtUtil {

    /**
     * Get specified claim's string value from JWT
     */
    public static String getClaimAsString(String token, String claimName) {
        // JWT format: header.payload.signature
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            log.error("Invalid JWT format");
            return null;
        }

        // Decode payload part (Base64URL)
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

        // Parse JSON
        JsonNode jsonNode = JsonUtil.readTree(payload);
        JsonNode claimNode = jsonNode.get(claimName);

        return claimNode != null ? claimNode.asText() : null;
    }
}

