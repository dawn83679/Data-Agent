package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolResult {

    private static final String DEFAULT_SUCCESS_MESSAGE =
            "The tool completed successfully. Use this result for the next step.";

    private static final String DEFAULT_EMPTY_MESSAGE =
            "No matching results were found. Adjust the scope or ask the user to clarify the target before proceeding.";

    private static final String DEFAULT_FAIL_MESSAGE =
            "The tool failed. Review the current inputs and context before retrying.";

    private boolean success;

    private String message;

    private Object result;

    private Long elapsedMs;

    public static AgentToolResult success(Object result) {
        return success(result, DEFAULT_SUCCESS_MESSAGE);
    }

    public static AgentToolResult success(Object result, String message) {
        return AgentToolResult.builder()
                .success(true)
                .message(defaultMessage(message, DEFAULT_SUCCESS_MESSAGE))
                .result(result)
                .build();
    }

    public static AgentToolResult empty() {
        return empty(DEFAULT_EMPTY_MESSAGE);
    }

    public static AgentToolResult empty(String message) {
        return AgentToolResult.builder()
                .success(true)
                .message(defaultMessage(message, DEFAULT_EMPTY_MESSAGE))
                .build();
    }

    public static AgentToolResult noContext() {
        return AgentToolResult.builder()
                .success(false)
                .message(ToolMessageSupport.sentence(
                        "Internal error: user session context is not available.",
                        "This is a system issue - do not retry.",
                        "Report the problem to the user."
                ))
                .build();
    }

    public static AgentToolResult fail(String error) {
        return AgentToolResult.builder()
                .success(false)
                .message(defaultMessage(error, DEFAULT_FAIL_MESSAGE))
                .build();
    }

    public static AgentToolResult fail(String customMessage, String errorDetail) {
        return AgentToolResult.builder()
                .success(false)
                .message(defaultMessage(customMessage, DEFAULT_FAIL_MESSAGE)
                        + " Error: " + defaultMessage(errorDetail, "unknown error"))
                .build();
    }

    public static AgentToolResult fail(Throwable e) {
        return fail(e != null ? e.getMessage() : null);
    }

    private static String defaultMessage(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }
}
