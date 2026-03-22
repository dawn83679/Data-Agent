package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

/**
 * Long-term memory lifecycle status.
 */
@Getter
public enum MemoryStatusEnum {

    ACTIVE(0),
    ARCHIVED(1),
    HIDDEN(2);

    private final int code;

    MemoryStatusEnum(int code) {
        this.code = code;
    }

    public static MemoryStatusEnum fromCode(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (MemoryStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported memory status code: " + code);
    }
}
