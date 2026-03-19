package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryReviewStateEnum {

    USER_CONFIRMED("USER_CONFIRMED", 0),
    NEEDS_REVIEW("NEEDS_REVIEW", 1);

    private final String code;

    private final int priority;

    public static MemoryReviewStateEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.code.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static String validCodes() {
        return Arrays.stream(values())
                .map(MemoryReviewStateEnum::getCode)
                .collect(Collectors.joining(", "));
    }
}
