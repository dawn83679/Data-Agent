package edu.zsc.ai.agent.tool.model;

import lombok.Builder;
import lombok.Data;

/**
 * Standardized return type for agent tool methods.
 *
 * <p>Three states:
 * <ul>
 *   <li>{@link #success} — tool executed and returned data; model should continue answering.</li>
 *   <li>{@link #empty}   — tool executed but found nothing; model should reconsider parameters.</li>
 *   <li>{@link #fail}    — tool threw an error; model should reconsider or escalate to user.</li>
 * </ul>
 */
@Data
@Builder
public class AgentToolResult {

    private static final String MSG_SUCCESS =
            "Tool executed successfully. Use the result to continue answering the user's request.";

    private static final String MSG_EMPTY =
            "The tool returned no results. Reconsider your input parameters (e.g. try a different " +
            "pattern, value, or scope), or call askUserQuestion to ask the user for more specific information.";

    private static final String MSG_FAIL =
            "The tool encountered an error. Reconsider your parameters or approach, " +
            "or call askUserQuestion to ask the user for clarification. Error: ";

    private boolean success;

    private String message;

    private Object result;

    public static AgentToolResult success(Object result) {
        return AgentToolResult.builder()
                .success(true)
                .message(MSG_SUCCESS)
                .result(result)
                .build();
    }

    public static AgentToolResult empty() {
        return AgentToolResult.builder()
                .success(true)
                .message(MSG_EMPTY)
                .build();
    }

    public static AgentToolResult noContext() {
        return AgentToolResult.builder()
                .success(false)
                .message("User context is missing. This is a system error — please retry or contact support.")
                .build();
    }

    public static AgentToolResult fail(String error) {
        return AgentToolResult.builder()
                .success(false)
                .message(MSG_FAIL + error)
                .build();
    }

    public static AgentToolResult fail(String customMessage, String errorDetail) {
        return AgentToolResult.builder()
                .success(false)
                .message(customMessage + " Error: " + errorDetail)
                .build();
    }

    public static AgentToolResult fail(Throwable e) {
        return AgentToolResult.builder()
                .success(false)
                .message(MSG_FAIL + e.getMessage())
                .build();
    }
}
