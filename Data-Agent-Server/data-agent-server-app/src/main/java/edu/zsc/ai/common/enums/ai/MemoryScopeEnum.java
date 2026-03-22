package edu.zsc.ai.common.enums.ai;

import java.util.Locale;
import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryScopeEnum {

    CONVERSATION("CONVERSATION", 0),
    USER("USER", 1);

    private final String code;
    private final int priority;

    public static MemoryScopeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (MemoryScopeEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    public boolean matches(String code) {
        return this == fromCode(code);
    }

    public static String validCodes() {
        return Arrays.stream(values())
                .map(MemoryScopeEnum::getCode)
                .collect(Collectors.joining(", "));
    }
}
