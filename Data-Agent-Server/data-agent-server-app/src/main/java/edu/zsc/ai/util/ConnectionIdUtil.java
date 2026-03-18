package edu.zsc.ai.util;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified String/Long type conversion for connectionId, conversationId, userId, etc.
 * InvocationParameters may contain Strings (from LLM tool params) while RequestContext/Service layer expects Long.
 */
public final class ConnectionIdUtil {

    private ConnectionIdUtil() {
    }

    /**
     * Safely converts an Object to Long. Supports null, Long, Number, String.
     */
    public static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            if (s.isEmpty()) return null;
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Safely converts an Object to a list of Longs. Supports null, Number, String, and Collection.
     */
    public static List<Long> toLongList(Object obj) {
        if (obj == null) return List.of();
        if (obj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(ConnectionIdUtil::toLong)
                    .filter(item -> item != null)
                    .toList();
        }
        if (obj instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) return List.of();
            if (normalized.startsWith("[") && normalized.endsWith("]")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            if (normalized.isBlank()) return List.of();
            return Arrays.stream(normalized.split(","))
                    .map(String::trim)
                    .map(ConnectionIdUtil::toLong)
                    .filter(item -> item != null)
                    .toList();
        }
        Long single = toLong(obj);
        return single != null ? List.of(single) : List.of();
    }

    public static String toCsv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
