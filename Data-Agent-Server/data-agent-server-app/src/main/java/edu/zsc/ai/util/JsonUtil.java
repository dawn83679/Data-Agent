package edu.zsc.ai.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON Utility Class based on Jackson ObjectMapper
 * Provides convenient JSON conversion methods.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get the shared ObjectMapper instance
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public static String map2Json(Map map) {
        if (MapUtils.isEmpty(map)) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert map to JSON string", e);
            return "{}";
        }
    }


    public static Map<String, String> json2Map(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON string to map: {}", json, e);
            return new HashMap<>();
        }
    }

    public static String object2json(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON string", e);
            return "{}";
        }
    }

    public static <T> T json2Object(@NotBlank String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decode JWT token payload without signature verification
     * Note: This method does NOT verify the JWT signature. Use only when signature verification
     * is handled separately or when working with trusted token sources.
     *
     * @param idToken JWT token string
     * @return Map containing the decoded claims from the JWT payload
     * @throws IllegalArgumentException if token format is invalid
     */
    public static Map<String, Object> decodeJwtPayload(String idToken) {
        if (StringUtils.isBlank(idToken)) {
            throw new IllegalArgumentException("JWT token cannot be null or empty");
        }

        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format - expected 3 parts separated by dots");
        }

        try {
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            
            // Parse JSON to extract claims
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            
            return claims;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to decode JWT payload - invalid Base64 encoding", e);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JWT payload as JSON", e);
        }
    }

    /**
     * URL encode a string using UTF-8 encoding
     *
     * @param value String to encode
     * @return URL-encoded string, or empty string if input is null
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}