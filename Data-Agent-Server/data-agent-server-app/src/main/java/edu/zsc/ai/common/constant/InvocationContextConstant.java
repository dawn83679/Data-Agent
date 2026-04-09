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
    /** Main agent: connection ids returned by {@code getAllConnections} for this chat (tool-thread access check). */
    public static final String READABLE_CONNECTION_IDS = "readableConnectionIds";
    public static final String MODEL_NAME = "modelName";
    public static final String LANGUAGE = "language";

    public static final String WORKSPACE_TYPE = "workspaceType";
    public static final String ORG_ID = "orgId";
    public static final String ORG_USER_REL_ID = "orgUserRelId";
    public static final String ORG_ROLE = "orgRole";
}
