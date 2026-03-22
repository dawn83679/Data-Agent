package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.CompressionService;
import edu.zsc.ai.domain.service.ai.model.CompressionDoneMetadata;
import edu.zsc.ai.domain.service.ai.model.CompressionResult;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMemoryCompressorTest {

    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final AiMessageService aiMessageService = mock(AiMessageService.class);
    private final CompressionService compressionService = mock(CompressionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AgentLogService agentLogService = mock(AgentLogService.class);

    private final ChatMemoryCompressor compressor = new ChatMemoryCompressor(
            aiConversationService,
            aiMessageService,
            compressionService,
            eventPublisher,
            agentLogService
    );

    @Test
    void compressIfNeeded_updatesConversationTokenCountFromCompressionOutputTokens() {
        Long conversationId = 415L;
        when(aiConversationService.getById(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .tokenCount(740470)
                .build());
        when(compressionService.compress(any())).thenReturn(new CompressionResult(
                "## Active Context\n- compressed",
                4821,
                1350
        ));

        List<ChatMessage> messages = List.of(
                UserMessage.from("u1"),
                UserMessage.from("u2"),
                UserMessage.from("u3"),
                UserMessage.from("u4")
        );

        List<ChatMessage> result = compressor.compressIfNeeded(conversationId, "qwen3-max", messages);

        verify(aiConversationService).updateTokenCount(conversationId, 1350);
        assertEquals(2, result.size());
        assertTrue(ChatMemoryCompressor.isSummaryMessage(result.get(0)));
        assertEquals("u4", ((UserMessage) result.get(1)).singleText());
    }

    @Test
    void compressIfNeeded_storesDoneMetadataAndRuntimeLogFields() {
        Long conversationId = 415L;
        when(aiConversationService.getById(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .tokenCount(740470)
                .build());
        when(compressionService.compress(any())).thenReturn(new CompressionResult(
                "## Active Context\n- compressed",
                4821,
                1350
        ));

        compressor.compressIfNeeded(conversationId, "qwen3-max", List.of(
                UserMessage.from("u1"),
                UserMessage.from("u2"),
                UserMessage.from("u3"),
                UserMessage.from("u4")
        ));

        Map<String, Object> doneMetadata = compressor.consumeDoneMetadata(conversationId);
        assertEquals(Boolean.TRUE, doneMetadata.get("memoryCompressed"));
        assertEquals(740470, doneMetadata.get("tokenCountBefore"));
        assertEquals(1350, doneMetadata.get("tokenCountAfter"));
        assertEquals(3, doneMetadata.get("compressedMessageCount"));
        assertEquals(1, doneMetadata.get("keptRecentCount"));
        assertEquals(1350, doneMetadata.get("compressionOutputTokens"));
        assertEquals(4821, doneMetadata.get("compressionTotalTokens"));
        assertTrue(compressor.consumeDoneMetadata(conversationId).isEmpty());

        ArgumentCaptor<AgentLogEvent> captor = ArgumentCaptor.forClass(AgentLogEvent.class);
        verify(agentLogService, atLeastOnce()).record(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(event ->
                "compression_started".equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())));
        assertTrue(captor.getAllValues().stream().anyMatch(event ->
                "compression_completed".equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())
                        && Integer.valueOf(1350).equals(event.getPayload().get("tokenCountAfter"))));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void compressNow_replacesConversationMessagesAndReturnsCompressionStats() {
        Long conversationId = 512L;
        when(aiConversationService.getByIdForCurrentUser(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .tokenCount(6400)
                .build());
        when(compressionService.compress(any())).thenReturn(new CompressionResult(
                "## Execution State\n- Task: compressed",
                2100,
                700
        ));
        when(aiMessageService.getByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(
                stored(conversationId, UserMessage.from("u1")),
                stored(conversationId, UserMessage.from("u2")),
                stored(conversationId, UserMessage.from("u3")),
                stored(conversationId, UserMessage.from("u4"))
        ));

        CompressionDoneMetadata result = compressor.compressNow(conversationId, "qwen3.5-plus");

        assertEquals(Boolean.TRUE, result.memoryCompressed());
        assertEquals(6400, result.tokenCountBefore());
        assertEquals(null, result.tokenCountAfter());
        assertEquals(3, result.compressedMessageCount());
        assertEquals(1, result.keptRecentCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(aiMessageService).replaceConversationMessages(eq(conversationId), captor.capture());
        verify(aiConversationService, never()).updateTokenCount(conversationId, 700);
        assertEquals(2, captor.getValue().size());
        assertTrue(ChatMemoryCompressor.isSummaryMessage(captor.getValue().get(0)));
        assertTrue(compressor.consumeDoneMetadata(conversationId).isEmpty());
    }

    private StoredChatMessage stored(Long conversationId, ChatMessage message) {
        return StoredChatMessage.builder()
                .conversationId(conversationId)
                .data(ChatMessageSerializer.messageToJson(message))
                .build();
    }
}
