package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum MessagePriorityEnum {

    NORMAL(0),
    SUMMARY(1);

    private final int code;

    MessagePriorityEnum(int code) {
        this.code = code;
    }

    public static MessagePriorityEnum fromCode(int code) {
        for (MessagePriorityEnum priority : values()) {
            if (priority.code == code) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown priority code: " + code);
    }
}
