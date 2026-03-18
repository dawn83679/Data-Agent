package edu.zsc.ai.common.constant;

/**
 * JSON payload keys used by {@code ChatResponseBlock} and downstream stream consumers.
 */
public final class ChatResponseDataKey {

    private ChatResponseDataKey() {
    }

    public static final String ID = "id";
    public static final String TOOL_NAME = "toolName";
    public static final String ARGUMENTS = "arguments";
    public static final String RESULT = "result";
    public static final String ERROR = "error";
    public static final String STREAMING = "streaming";

    public static final String AGENT_TYPE = "agentType";
    public static final String MESSAGE = "message";
    public static final String TOOL_COUNT = "toolCount";
    public static final String TOOL_COUNTS = "toolCounts";
    public static final String SUMMARY_TEXT = "summaryText";
    public static final String RESULT_JSON = "resultJson";
    public static final String CONNECTION_ID = "connectionId";
    public static final String TIMEOUT_SECONDS = "timeoutSeconds";
}
