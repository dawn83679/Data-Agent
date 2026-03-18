package edu.zsc.ai.agent.tool.error;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

@Component
@Slf4j
public class ToolErrorMapper {

    public Object mapFailure(Method method, Object[] args, Throwable throwable) {
        Throwable cause = unwrap(throwable);
        Class<?> returnType = method.getReturnType();
        String toolName = resolveToolName(method, cause);
        String sanitizedArgs = resolveSanitizedArgs(args, cause);
        String messageForModel = resolveMessageForModel(toolName, sanitizedArgs, cause);

        logFailure(toolName, sanitizedArgs, cause, messageForModel);
        return buildFailurePayload(returnType, messageForModel, toolName);
    }

    private Object buildFailurePayload(Class<?> returnType, String messageForModel, String toolName) {
        if (AgentToolResult.class.isAssignableFrom(returnType)) {
            return AgentToolResult.fail(messageForModel);
        }
        if (AgentSqlResult.class.isAssignableFrom(returnType)) {
            return AgentSqlResult.fail(messageForModel);
        }
        if (AskUserConfirmTool.WriteConfirmationResult.class.isAssignableFrom(returnType)) {
            return AskUserConfirmTool.WriteConfirmationResult.error(messageForModel);
        }
        if (String.class.equals(returnType)) {
            return messageForModel;
        }
        throw new IllegalStateException("No tool error mapping registered for return type "
                + returnType.getName() + " (tool=" + toolName + ")");
    }

    private String resolveToolName(Method method, Throwable cause) {
        if (cause instanceof AgentToolExecuteException toolException && toolException.getToolName() != null) {
            return toolException.getToolName().getToolName();
        }
        return ToolNameEnum.fromToolName(method.getName())
                .map(ToolNameEnum::getToolName)
                .orElse(method.getName());
    }

    private String resolveSanitizedArgs(Object[] args, Throwable cause) {
        if (cause instanceof AgentToolExecuteException toolException && StringUtils.isNotBlank(toolException.getSanitizedArgs())) {
            return toolException.getSanitizedArgs();
        }
        if (args == null || args.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof InvocationParameters) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("arg").append(i).append("=").append(sanitizeValue(arg));
        }
        return builder.toString();
    }

    private String resolveMessageForModel(String toolName, String sanitizedArgs, Throwable cause) {
        if (cause instanceof AgentToolExecuteException toolException) {
            return StringUtils.defaultIfBlank(toolException.getMessageForModel(), defaultUnexpectedMessage(toolName));
        }
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            return buildValidationMessage(toolName, cause.getMessage(), sanitizedArgs);
        }
        return buildExecutionMessage(toolName, cause, sanitizedArgs);
    }

    private void logFailure(String toolName, String sanitizedArgs, Throwable cause, String messageForModel) {
        if (cause instanceof AgentToolExecuteException toolException) {
            log.warn("[Tool mapped error] tool={}, code={}, retryable={}, args={}, modelMessage={}, logMessage={}",
                    toolName,
                    toolException.getErrorCode(),
                    toolException.isRetryable(),
                    sanitizedArgs,
                    messageForModel,
                    toolException.getMessageForLog());
            return;
        }
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            log.warn("[Tool mapped error] tool={}, args={}, message={}", toolName, sanitizedArgs, messageForModel);
            return;
        }
        log.error("[Tool mapped error] tool={}, args={}, message={}", toolName, sanitizedArgs, messageForModel, cause);
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable.getCause() != null && throwable instanceof InvocationTargetException) {
            return unwrap(throwable.getCause());
        }
        return throwable;
    }

    private String defaultUnexpectedMessage(String toolName) {
        return ToolMessageSupport.sentence(
                "Tool " + toolName + " failed before it could return a usable result.",
                "Review the current inputs and context, then retry.",
                ToolMessageSupport.IF_THE_TARGET_IS_STILL_UNCLEAR_ASK_THE_USER_BEFORE_PROCEEDING
        );
    }

    private String buildValidationMessage(String toolName, String rawMessage, String sanitizedArgs) {
        String detail = StringUtils.defaultIfBlank(rawMessage, "invalid input or missing context");
        String context = formatArgsForModel(sanitizedArgs);
        return ToolMessageSupport.sentence(
                "Tool " + toolName + " rejected the current input" + context + ".",
                "Error: " + detail + ".",
                "Fix the parameters or missing context before retrying.",
                "Do not continue with dependent steps until this is resolved."
        );
    }

    private String buildExecutionMessage(String toolName, Throwable cause, String sanitizedArgs) {
        String detail = StringUtils.defaultIfBlank(cause != null ? cause.getMessage() : null,
                cause != null ? cause.getClass().getSimpleName() : null);
        String context = formatArgsForModel(sanitizedArgs);
        if (StringUtils.isBlank(detail)) {
            return defaultUnexpectedMessage(toolName);
        }
        return ToolMessageSupport.sentence(
                "Tool " + toolName + " failed" + context + ".",
                "Error: " + detail + ".",
                "Review the current scope and retry.",
                ToolMessageSupport.IF_THE_TARGET_IS_STILL_UNCLEAR_ASK_THE_USER_BEFORE_PROCEEDING
        );
    }

    private String formatArgsForModel(String sanitizedArgs) {
        if (StringUtils.isBlank(sanitizedArgs)) {
            return StringUtils.EMPTY;
        }
        return " while handling " + sanitizedArgs;
    }

    private String sanitizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence text) {
            String normalized = text.toString().replaceAll("\\s+", " ").trim();
            return normalized.length() > 120 ? normalized.substring(0, 117) + "..." : normalized;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + CollectionUtils.size(collection) + ")";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "Array(length=" + Array.getLength(value) + ")";
        }
        return Optional.ofNullable(value.getClass().getSimpleName()).filter(StringUtils::isNotBlank).orElse("Object");
    }
}
