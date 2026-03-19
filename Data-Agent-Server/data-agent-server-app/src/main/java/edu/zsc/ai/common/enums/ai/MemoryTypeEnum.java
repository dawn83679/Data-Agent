package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported long-term memory types.
 */
@Getter
@RequiredArgsConstructor
public enum MemoryTypeEnum {

    PREFERENCE("PREFERENCE"),
    BUSINESS_RULE("BUSINESS_RULE"),
    KNOWLEDGE_POINT("KNOWLEDGE_POINT"),
    GOLDEN_SQL_CASE("GOLDEN_SQL_CASE"),
    WORKFLOW_CONSTRAINT("WORKFLOW_CONSTRAINT");

    private final String code;

    public static MemoryTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(v -> v.code.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static String validCodes() {
        return Arrays.stream(values())
                .map(MemoryTypeEnum::getCode)
                .collect(Collectors.joining(", "));
    }

    public boolean matches(String code) {
        return this == fromCode(code);
    }
}
