package edu.zsc.ai.observability;

public enum AgentLogType {

    CONVERSATION_START(AgentLogCategory.GENERAL),
    CONVERSATION_COMPLETE(AgentLogCategory.GENERAL),
    CONVERSATION_ERROR(AgentLogCategory.GENERAL),
    CHAT_BLOCK(AgentLogCategory.SSE),
    TOOL_METHOD_START(AgentLogCategory.TOOL),
    TOOL_METHOD_COMPLETE(AgentLogCategory.TOOL),
    TOOL_METHOD_ERROR(AgentLogCategory.TOOL),
    SUB_AGENT_START(AgentLogCategory.GENERAL),
    SUB_AGENT_COMPLETE(AgentLogCategory.GENERAL),
    SUB_AGENT_ERROR(AgentLogCategory.GENERAL),
    SUB_AGENT_TIMEOUT(AgentLogCategory.GENERAL),
    TOKEN_PARTIAL_RESPONSE(AgentLogCategory.TOKEN),
    TOKEN_PARTIAL_THINKING(AgentLogCategory.TOKEN),
    TOKEN_TOOL_CALL(AgentLogCategory.TOKEN),
    TOKEN_TOOL_RESULT(AgentLogCategory.TOKEN),
    TOKEN_COMPLETE(AgentLogCategory.TOKEN),
    TOKEN_ERROR(AgentLogCategory.TOKEN),
    PROMPT_ORIGINAL_USER_INPUT(AgentLogCategory.PROMPT),
    PROMPT_RENDERED_USER(AgentLogCategory.PROMPT),
    EXECUTOR_TASK_START(AgentLogCategory.DEBUG),
    EXECUTOR_TASK_COMPLETE(AgentLogCategory.DEBUG),
    EXECUTOR_TASK_ERROR(AgentLogCategory.DEBUG),
    DEBUG_EVENT(AgentLogCategory.DEBUG);

    private final AgentLogCategory category;

    AgentLogType(AgentLogCategory category) {
        this.category = category;
    }

    public AgentLogCategory getCategory() {
        return category;
    }
}
