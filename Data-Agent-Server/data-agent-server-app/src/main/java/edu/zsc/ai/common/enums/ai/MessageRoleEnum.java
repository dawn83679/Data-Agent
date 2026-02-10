package edu.zsc.ai.common.enums.ai;

/**
 * Message role for API/frontend. Maps backend (e.g. LangChain4j) role names to frontend values.
 */
public enum MessageRoleEnum {

    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    MessageRoleEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Maps backend role type (e.g. USER, AI, TOOL_EXECUTION_RESULT) to frontend role enum.
     */
    public static MessageRoleEnum fromBackendType(String type) {
        return USER.name().equals(type) ? USER : ASSISTANT;
    }
}
