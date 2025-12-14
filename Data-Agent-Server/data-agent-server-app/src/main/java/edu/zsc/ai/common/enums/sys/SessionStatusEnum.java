package edu.zsc.ai.common.enums.sys;

/**
 * Session status enum
 * 
 * @author zgq
 * @since 2025-10-01
 */
public enum SessionStatusEnum {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    SessionStatusEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

