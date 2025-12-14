package edu.zsc.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

@Slf4j
public class JwtUtil {

    /**
     * Get claim as String from JWT token (without verification)
     */
    public static String getClaimAsString(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode jsonNode = JsonUtil.readTree(payload);
            if (jsonNode != null && jsonNode.has(claim)) {
                return jsonNode.get(claim).asText();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            return null;
        }
    }
}
