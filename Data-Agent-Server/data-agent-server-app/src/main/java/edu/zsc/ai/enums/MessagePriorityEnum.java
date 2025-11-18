package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Message priority enumeration
 *
 * @author zgq
 */
@Getter
public enum MessagePriorityEnum {

    NORMAL(0, "Normal message"),
    SUMMARY(1, "Summary message");

    private final Integer value;
    private final String description;

    MessagePriorityEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Get enum by value
     *
     * @param value priority value
     * @return message priority enum
     */
    public static MessagePriorityEnum fromValue(Integer value) {
        for (MessagePriorityEnum priority : values()) {
            if (priority.getValue().equals(value)) {
                return priority;
            }
        }
        return NORMAL; // Default to normal
    }
}