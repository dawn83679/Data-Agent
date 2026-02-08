package edu.zsc.ai.common.enums.ai;

import lombok.Getter;


@Getter
public enum MessageStatusEnum {

    NORMAL(0),
    DELETED(1),
    COMPRESSED(2);

    private final int code;

    MessageStatusEnum(int code) {
        this.code = code;
    }

    public static MessageStatusEnum fromCode(int code) {
        for (MessageStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
