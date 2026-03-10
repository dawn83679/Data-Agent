package edu.zsc.ai.common.constant;

/**
 * Keys for request context map (e.g. when passing to AI Service InvocationParameters).
 */
public final class RequestContextConstant {

    private RequestContextConstant() {
    }

    public static final String USER_ID = "userId";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String CONNECTION_ID = "connectionId";
    public static final String DATABASE_NAME = "databaseName";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String AGENT_MODE = "agentMode";
    public static final String MODEL_NAME = "modelName";
    public static final String LANGUAGE = "language";
    public static final String RUN_ID = "runId";
    public static final String TASK_ID = "taskId";
    public static final String AGENT_ROLE = "agentRole";
    public static final String PARENT_AGENT_ROLE = "parentAgentRole";
}
