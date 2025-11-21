package edu.zsc.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

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
}