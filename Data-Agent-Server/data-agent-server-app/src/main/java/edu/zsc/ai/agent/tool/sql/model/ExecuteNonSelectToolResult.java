package edu.zsc.ai.agent.tool.sql.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecuteNonSelectToolResult {

    public enum Status {
        EXECUTED,
        REQUIRES_CONFIRMATION
    }

    private Status status;
    private boolean ruleMatched;
    private boolean requiresConfirmation;
    private WriteExecutionConfirmationPayload confirmation;
    private AgentSqlResult execution;
    private String message;

    public static ExecuteNonSelectToolResult executed(boolean ruleMatched,
                                                      AgentSqlResult execution,
                                                      String message) {
        return ExecuteNonSelectToolResult.builder()
                .status(Status.EXECUTED)
                .ruleMatched(ruleMatched)
                .requiresConfirmation(false)
                .execution(execution)
                .message(message)
                .build();
    }

    public static ExecuteNonSelectToolResult requiresConfirmation(WriteExecutionConfirmationPayload confirmation,
                                                                  String message) {
        return ExecuteNonSelectToolResult.builder()
                .status(Status.REQUIRES_CONFIRMATION)
                .ruleMatched(false)
                .requiresConfirmation(true)
                .confirmation(confirmation)
                .message(message)
                .build();
    }
}
