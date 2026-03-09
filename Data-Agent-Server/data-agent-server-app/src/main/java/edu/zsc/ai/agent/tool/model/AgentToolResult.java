package edu.zsc.ai.agent.tool.model;

import lombok.Builder;
import lombok.Data;

import java.util.function.Supplier;

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

    private static final String MSG_SUCCESS = "ok";

    private static final String MSG_EMPTY = "no results; try different params or ask user";

    private static final String MSG_FAIL = "error: ";

    private boolean success;

    private String message;

    private Object result;

    private Long elapsedMs;

    public static AgentToolResult timed(Supplier<AgentToolResult> action) {
        long start = System.currentTimeMillis();
        AgentToolResult result = action.get();
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

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
                .message("Internal error: user session context is not available. "
                        + "This is a system issue — do not retry. Report the problem to the user.")
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
