package edu.zsc.ai.context;

public final class AgentExecutionContext {

    private static final ThreadLocal<String> PARENT_TOOL_CALL_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TASK_ID = new ThreadLocal<>();

    private AgentExecutionContext() {
    }

    public static void setParentToolCallId(String id) {
        if (id == null) {
            PARENT_TOOL_CALL_ID.remove();
            return;
        }
        PARENT_TOOL_CALL_ID.set(id);
    }

    public static String getParentToolCallId() {
        return PARENT_TOOL_CALL_ID.get();
    }

    public static void setTaskId(String id) {
        if (id == null) {
            TASK_ID.remove();
            return;
        }
        TASK_ID.set(id);
    }

    public static String getTaskId() {
        return TASK_ID.get();
    }

    public static void clear() {
        PARENT_TOOL_CALL_ID.remove();
        TASK_ID.remove();
    }
}
