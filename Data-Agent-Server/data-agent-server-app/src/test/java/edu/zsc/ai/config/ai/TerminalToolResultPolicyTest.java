package edu.zsc.ai.config.ai;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalToolResultPolicyTest {

    private final TerminalToolResultPolicy policy = new TerminalToolResultPolicy();

    @Test
    void shouldShortCircuitForSuccessfulRenderChartResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("画一张图"),
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.RENDER_CHART.getToolName(),
                                "{\"success\":true,\"message\":\"Chart ready\"}")
                ))
                .build();

        assertTrue(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldShortCircuitForSuccessfulExportFileResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("导出文件"),
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXPORT_FILE.getToolName(),
                                "{\"success\":true,\"message\":\"File ready\"}")
                ))
                .build();

        assertTrue(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldShortCircuitForWriteConfirmationResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        UserMessage.from("删除一条订单"),
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName(),
                                """
                                        {"status":"REQUIRES_CONFIRMATION","requiresConfirmation":true,"confirmation":{"conversationId":1,"connectionId":2,"sql":"DELETE FROM orders WHERE id = 1","sqlPreview":"DELETE FROM orders WHERE id = 1","availableGrantOptions":[{"scopeType":"CONVERSATION","grantPreset":"CONNECTION_ALL_DATABASES"}]},"message":"confirmation required"}
                                        """)
                ))
                .build();

        assertTrue(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitForExecutedWriteResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName(),
                                "{\"status\":\"EXECUTED\",\"requiresConfirmation\":false,\"execution\":{\"success\":true}}")
                ))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitForInvalidWriteConfirmationPayload() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName(),
                                "{\"status\":\"REQUIRES_CONFIRMATION\",\"requiresConfirmation\":true}")
                ))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitWhenTerminalToolPayloadFailed() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.RENDER_CHART.getToolName(),
                                "{\"success\":false,\"message\":\"Invalid optionJson\"}")
                ))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitWhenToolResultIsError() {
        ToolExecutionResultMessage failedResult = ToolExecutionResultMessage.builder()
                .id("call-1")
                .toolName(ToolNameEnum.EXPORT_FILE.getToolName())
                .text("{\"success\":true}")
                .isError(true)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(failedResult))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitForNonTerminalToolResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.GET_DATABASES.getToolName(),
                                "{\"success\":true,\"message\":\"ok\"}")
                ))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitWhenAnyTrailingToolResultFailed() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.RENDER_CHART.getToolName(),
                                "{\"success\":true,\"message\":\"Chart ready\"}"),
                        ToolExecutionResultMessage.from("call-2", ToolNameEnum.GET_DATABASES.getToolName(),
                                "{\"success\":false,\"message\":\"Database unavailable\"}")
                ))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }

    @Test
    void shouldNotShortCircuitWhenLastMessageIsNotToolResult() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from("hello")))
                .build();

        assertFalse(policy.shouldShortCircuit(request));
    }
}
