package edu.zsc.ai.config.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalToolAwareStreamingChatModelTest {

    private final TerminalToolResultPolicy policy = new TerminalToolResultPolicy();

    @Test
    void successfulTerminalToolResultCompletesWithoutCallingDelegate() {
        RecordingStreamingChatModel delegate = new RecordingStreamingChatModel();
        TerminalToolAwareStreamingChatModel model = new TerminalToolAwareStreamingChatModel(delegate, policy);
        CapturingHandler handler = new CapturingHandler();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.RENDER_CHART.getToolName(),
                                "{\"success\":true,\"message\":\"Chart ready\"}")
                ))
                .build();

        model.chat(request, handler);

        assertFalse(delegate.called);
        assertNotNull(handler.response);
        assertEquals("", handler.response.aiMessage().text());
        assertEquals(FinishReason.STOP, handler.response.finishReason());
        assertTrue(handler.partialResponses.isEmpty());
    }

    @Test
    void failedTerminalToolResultDelegatesToModelForRetry() {
        RecordingStreamingChatModel delegate = new RecordingStreamingChatModel();
        TerminalToolAwareStreamingChatModel model = new TerminalToolAwareStreamingChatModel(delegate, policy);
        CapturingHandler handler = new CapturingHandler();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXPORT_FILE.getToolName(),
                                "{\"success\":false,\"message\":\"Missing data\"}")
                ))
                .build();

        model.chat(request, handler);

        assertTrue(delegate.called);
        assertNotNull(handler.response);
        assertEquals("delegate", handler.response.aiMessage().text());
        assertEquals(List.of("delegate"), handler.partialResponses);
    }

    @Test
    void writeConfirmationCompletesWithoutCallingDelegate() {
        RecordingStreamingChatModel delegate = new RecordingStreamingChatModel();
        TerminalToolAwareStreamingChatModel model = new TerminalToolAwareStreamingChatModel(delegate, policy);
        CapturingHandler handler = new CapturingHandler();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ToolExecutionResultMessage.from("call-1", ToolNameEnum.EXECUTE_NON_SELECT_SQL.getToolName(),
                                """
                                        {"status":"REQUIRES_CONFIRMATION","requiresConfirmation":true,"confirmation":{"conversationId":1,"connectionId":2,"sql":"DELETE FROM orders WHERE id = 1","sqlPreview":"DELETE FROM orders WHERE id = 1","availableGrantOptions":[{"scopeType":"CONVERSATION","grantPreset":"CONNECTION_ALL_DATABASES"}]}}
                                        """)
                ))
                .build();

        model.chat(request, handler);

        assertFalse(delegate.called);
        assertNotNull(handler.response);
        assertEquals("", handler.response.aiMessage().text());
        assertEquals(FinishReason.STOP, handler.response.finishReason());
        assertTrue(handler.partialResponses.isEmpty());
    }

    private static class RecordingStreamingChatModel implements StreamingChatModel {

        private boolean called;

        @Override
        public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
            called = true;
            handler.onPartialResponse("delegate");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("delegate"))
                    .finishReason(FinishReason.STOP)
                    .build());
        }
    }

    private static class CapturingHandler implements StreamingChatResponseHandler {

        private final List<String> partialResponses = new ArrayList<>();
        private ChatResponse response;
        private Throwable error;

        @Override
        public void onPartialResponse(String partialResponse) {
            partialResponses.add(partialResponse);
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
            this.response = response;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
