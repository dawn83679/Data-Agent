package edu.zsc.ai.agent.tool.error;

import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import lombok.Getter;

@Getter
public class AgentToolExecuteException extends RuntimeException {

    private final ToolNameEnum toolName;
    private final ToolErrorCode errorCode;
    private final String messageForModel;
    private final String messageForLog;
    private final String sanitizedArgs;
    private final boolean retryable;

    public AgentToolExecuteException(ToolNameEnum toolName,
                                     ToolErrorCode errorCode,
                                     String messageForModel,
                                     String messageForLog,
                                     String sanitizedArgs,
                                     boolean retryable) {
        this(toolName, errorCode, messageForModel, messageForLog, sanitizedArgs, retryable, null);
    }

    public AgentToolExecuteException(ToolNameEnum toolName,
                                     ToolErrorCode errorCode,
                                     String messageForModel,
                                     String messageForLog,
                                     String sanitizedArgs,
                                     boolean retryable,
                                     Throwable cause) {
        super(messageForLog != null ? messageForLog : messageForModel, cause);
        this.toolName = toolName;
        this.errorCode = errorCode;
        this.messageForModel = messageForModel;
        this.messageForLog = messageForLog;
        this.sanitizedArgs = sanitizedArgs;
        this.retryable = retryable;
    }

    public static AgentToolExecuteException invalidInput(ToolNameEnum toolName, String messageForModel) {
        return new AgentToolExecuteException(toolName, ToolErrorCode.INVALID_INPUT, messageForModel, null, null, false);
    }

    public static AgentToolExecuteException preconditionFailed(ToolNameEnum toolName, String messageForModel) {
        return new AgentToolExecuteException(toolName, ToolErrorCode.PRECONDITION_FAILED, messageForModel, null, null, false);
    }

    public static AgentToolExecuteException scopeViolation(ToolNameEnum toolName, String messageForModel) {
        return new AgentToolExecuteException(toolName, ToolErrorCode.SCOPE_VIOLATION, messageForModel, null, null, false);
    }

    public static AgentToolExecuteException executionFailed(ToolNameEnum toolName,
                                                            String messageForModel,
                                                            String messageForLog,
                                                            boolean retryable,
                                                            Throwable cause) {
        return new AgentToolExecuteException(
                toolName,
                ToolErrorCode.EXECUTION_FAILED,
                messageForModel,
                messageForLog,
                null,
                retryable,
                cause
        );
    }
}
