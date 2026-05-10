package edu.zsc.ai.config.ai;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.util.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class TerminalToolResultPolicy {

    private static final String WRITE_CONFIRMATION_STATUS = "REQUIRES_CONFIRMATION";

    private static final Set<String> AGENT_RESULT_TERMINAL_TOOLS = Set.of(
            ToolNameEnum.RENDER_CHART.getToolName(),
            ToolNameEnum.EXPORT_FILE.getToolName()
    );

    public boolean shouldShortCircuit(ChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return false;
        }

        List<ChatMessage> messages = request.messages();
        boolean hasTerminalSuccess = false;
        boolean hasTrailingToolResult = false;

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (!(message instanceof ToolExecutionResultMessage toolResultMessage)) {
                break;
            }

            hasTrailingToolResult = true;
            ToolResultState state = classify(toolResultMessage);
            if (state == ToolResultState.FAILED) {
                return false;
            }
            if (state == ToolResultState.TERMINAL_SUCCESS) {
                hasTerminalSuccess = true;
            }
        }

        return hasTrailingToolResult && hasTerminalSuccess;
    }

    private ToolResultState classify(ToolExecutionResultMessage message) {
        if (Boolean.TRUE.equals(message.isError())) {
            return ToolResultState.FAILED;
        }

        Optional<Boolean> payloadSuccess = payloadSuccess(message.text());
        if (payloadSuccess.isPresent() && !payloadSuccess.get()) {
            return ToolResultState.FAILED;
        }

        if (isWriteConfirmation(message)) {
            return ToolResultState.TERMINAL_SUCCESS;
        }

        if (AGENT_RESULT_TERMINAL_TOOLS.contains(message.toolName()) && payloadSuccess.orElse(false)) {
            return ToolResultState.TERMINAL_SUCCESS;
        }

        return ToolResultState.SUCCESS;
    }

    private boolean isWriteConfirmation(ToolExecutionResultMessage message) {
        if (!ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName().equals(message.toolName())) {
            return false;
        }
        try {
            JsonNode root = JsonUtil.readTree(message.text());
            JsonNode status = root.get("status");
            JsonNode confirmation = root.get("confirmation");
            return status != null
                    && WRITE_CONFIRMATION_STATUS.equals(status.asText())
                    && confirmation != null
                    && confirmation.isObject();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Optional<Boolean> payloadSuccess(String text) {
        try {
            JsonNode successNode = JsonUtil.readTree(text).get("success");
            if (successNode != null && successNode.isBoolean()) {
                return Optional.of(successNode.booleanValue());
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private enum ToolResultState {
        SUCCESS,
        TERMINAL_SUCCESS,
        FAILED
    }
}
