package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

/**
 * Long-term memory enable status.
 */
@Getter
public enum MemoryEnableEnum {

    ENABLE(1),
    DISABLE(0);

    private final int code;

    MemoryEnableEnum(int code) {
        this.code = code;
    }

    public static MemoryEnableEnum fromCode(Integer code) {
        if (code == null) {
            return ENABLE;
        }
        for (MemoryEnableEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported memory enable code: " + code);
    }
}
