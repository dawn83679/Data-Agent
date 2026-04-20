package edu.zsc.ai.common.enums.ai;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Reserved for a future feature: scoping memories to a workspace hierarchy (global connection,
 * catalog, schema). Not persisted or referenced by the current memory pipeline; kept for
 * forward-compatible planning.
 */
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
