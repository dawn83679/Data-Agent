package edu.zsc.ai.common.enums.ai;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryWorkspaceLevelEnum {

    GLOBAL("GLOBAL", 0),
    CONNECTION("CONNECTION", 1),
    CATALOG("CATALOG", 2),
    SCHEMA("SCHEMA", 3);

    private final String code;
    private final int priority;

    public static MemoryWorkspaceLevelEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (MemoryWorkspaceLevelEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
