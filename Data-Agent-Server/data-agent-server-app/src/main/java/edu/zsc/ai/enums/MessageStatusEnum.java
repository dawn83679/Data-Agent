package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Message status enumeration
 *
 * @author zgq
 */
@Getter
public enum MessageStatusEnum {

    NORMAL(0, "Normal"),
    INVALID(1, "Invalid (manually deleted/rolled back)"),
    COMPRESSED(2, "Compressed");

    private final Integer value;
    private final String description;

    MessageStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Get enum by value
     *
     * @param value status value
     * @return message status enum
     */
    public static MessageStatusEnum fromValue(Integer value) {
        for (MessageStatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return NORMAL; // Default to normal
    }
}