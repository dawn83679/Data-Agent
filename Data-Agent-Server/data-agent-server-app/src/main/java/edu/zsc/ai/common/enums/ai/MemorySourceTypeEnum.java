package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemorySourceTypeEnum {

    MANUAL("MANUAL"),
    AGENT("AGENT");

    private final String code;

    public static MemorySourceTypeEnum fromCode(String code) {
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
                .map(MemorySourceTypeEnum::getCode)
                .collect(Collectors.joining(", "));
    }
}
