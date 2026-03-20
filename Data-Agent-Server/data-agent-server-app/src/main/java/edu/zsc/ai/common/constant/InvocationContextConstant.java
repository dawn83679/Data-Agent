package edu.zsc.ai.common.constant;

/**
 * Keys used in InvocationParameters when propagating request and agent context.
 */
public final class InvocationContextConstant {

    private InvocationContextConstant() {
    }

    public static final String USER_ID = "userId";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String CONNECTION_ID = "connectionId";
    public static final String DATABASE_NAME = "databaseName";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String AGENT_MODE = "agentMode";
    public static final String AGENT_TYPE = "agentType";
    public static final String ALLOWED_CONNECTION_IDS = "allowedConnectionIds";
    public static final String MODEL_NAME = "modelName";
    public static final String LANGUAGE = "language";
}
