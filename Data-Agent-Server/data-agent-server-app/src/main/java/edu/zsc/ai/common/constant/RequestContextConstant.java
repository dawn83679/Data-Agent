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
}
