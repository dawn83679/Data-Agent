package edu.zsc.ai.common.constant;

/**
 * Message constants returned by agent tools (e.g. user context missing, empty results).
 * Used by ConnectionTool, DatabaseTool, TableTool, ExecuteSqlTool.
 */
public final class ToolMessageConstants {

    private ToolMessageConstants() {}

    /** Returned when InvocationParameters does not contain userId. */
    public static final String USER_CONTEXT_MISSING = "User context missing.";

    /** Returned when getMyConnections returns no connections. */
    public static final String EMPTY_NO_CONNECTIONS = "EMPTY: No connections found.";

    /** Returned when listDatabases returns no databases. */
    public static final String EMPTY_NO_DATABASES = "EMPTY: No databases found.";

    /** Returned when getTableNames returns no tables. */
    public static final String EMPTY_NO_TABLES = "EMPTY: No tables found.";

    /** Returned when getViewNames returns no views. */
    public static final String EMPTY_NO_VIEWS = "EMPTY: No views found.";

    /** Returned when getFunctionNames returns no functions. */
    public static final String EMPTY_NO_FUNCTIONS = "EMPTY: No functions found.";

    /** Returned when getProcedureNames returns no procedures. */
    public static final String EMPTY_NO_PROCEDURES = "EMPTY: No stored procedures found.";

    /** Returned when getTriggerNames returns no triggers. */
    public static final String EMPTY_NO_TRIGGERS = "EMPTY: No triggers found.";

    /** Returned when getIndexes returns no indexes. */
    public static final String EMPTY_NO_INDEXES = "EMPTY: No indexes found.";

    /** Log prefix for tool entry. */
    public static final String TOOL_LOG_PREFIX_BEFORE = "[Tool before]";

    /** Log prefix for tool completion. */
    public static final String TOOL_LOG_PREFIX_DONE = "[Tool done]";

    /** Log prefix for tool error. */
    public static final String TOOL_LOG_PREFIX_ERROR = "[Tool error]";
}
