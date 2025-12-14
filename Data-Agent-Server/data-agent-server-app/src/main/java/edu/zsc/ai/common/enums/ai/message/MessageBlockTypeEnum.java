package edu.zsc.ai.common.enums.ai.message;

import lombok.Getter;

import java.util.Arrays;

/**
 * Message block type enum.
 *
 * @author zgq
 */
@Getter
public enum MessageBlockTypeEnum {

    /**
     * Normal text block
     */
    TEXT,

    /**
     * Summary block
     */
    SUMMARY,

    /**
     * Tool call block
     */
    TOOL_CALL,
    TOOL_CALL_REQUEST,

    /**
     * Tool result block
     */
    TOOL_CALL_RESULT,
    TOOL_CALL_REJECT,
    TOOL_CALL_DUPLICATE,
    TOOL_CALL_NOT_FOUND,
    TOOL_CALL_ERROR,
    TOOL_CALL_LIMIT_EXCEEDED,

    /**
     * Extra text error after tool call
     */
    TOOL_CALL_EXTRA_TEXT_ERROR,

    /**
     * Think block
     */
    THINK,
    ERROR,

    /**
     * API request
     */
    API_REQUEST_START,
    API_REQUEST_END,
    IMAGE;

    public static boolean isToolResult(String type) {
        return Arrays.asList(TOOL_CALL_RESULT.name(), TOOL_CALL_REJECT.name(),
                TOOL_CALL_DUPLICATE.name(), TOOL_CALL_NOT_FOUND.name(), TOOL_CALL_ERROR.name())
                .contains(type);
    }

    public static boolean isToolCall(String type) {
        return Arrays.asList(TOOL_CALL.name(), TOOL_CALL_REQUEST.name()).contains(type);
    }
}
