package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
<<<<<<< HEAD
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.AiModelProperties;
=======
import edu.zsc.ai.common.constant.CompressionLogConstant;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.CompressionService;
import edu.zsc.ai.domain.service.ai.model.CompressionDoneMetadata;
import edu.zsc.ai.domain.service.ai.model.CompressionResult;
<<<<<<< HEAD
=======
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
<<<<<<< HEAD
import static org.mockito.ArgumentMatchers.eq;
=======
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMemoryCompressorTest {

    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final AiMessageService aiMessageService = mock(AiMessageService.class);
    private final CompressionService compressionService = mock(CompressionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
<<<<<<< HEAD
    private final AiModelCatalog aiModelCatalog;

    private final ChatMemoryCompressor compressor;

    ChatMemoryCompressorTest() {
        AiModelProperties modelProperties = new AiModelProperties();
        aiModelCatalog = new AiModelCatalog(modelProperties);
        aiModelCatalog.initialize();
        compressor = new ChatMemoryCompressor(
                aiConversationService,
                aiMessageService,
                compressionService,
                eventPublisher,
                aiModelCatalog
        );
    }
=======
    private final AgentLogService agentLogService = mock(AgentLogService.class);

    private final ChatMemoryCompressor compressor = new ChatMemoryCompressor(
            aiConversationService,
            aiMessageService,
            compressionService,
            eventPublisher,
            agentLogService
    );
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793

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

        List<ChatMessage> result = compressor.compressIfNeeded(conversationId, "qwen3-max-2026-01-23", messages);

        verify(aiConversationService).updateTokenCount(conversationId, 1350);
        assertEquals(2, result.size());
        assertEquals("u4", ((UserMessage) result.get(0)).singleText());
        assertTrue(ChatMemoryCompressor.isSummaryMessage(result.get(1)));
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

        compressor.compressIfNeeded(conversationId, "qwen3-max-2026-01-23", List.of(
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
<<<<<<< HEAD
=======

        ArgumentCaptor<AgentLogEvent> captor = ArgumentCaptor.forClass(AgentLogEvent.class);
        verify(agentLogService, atLeastOnce()).record(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(event ->
                "compression_started".equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())));
        assertTrue(captor.getAllValues().stream().anyMatch(event ->
                "compression_completed".equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())
                        && Integer.valueOf(1350).equals(event.getPayload().get("tokenCountAfter"))));
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
        when(aiMessageService.getActiveByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(
                stored(conversationId, UserMessage.from("u1")),
                stored(conversationId, UserMessage.from("u2")),
                stored(conversationId, UserMessage.from("u3")),
                stored(conversationId, UserMessage.from("u4"))
        ));

        CompressionDoneMetadata result = compressor.compressNow(conversationId, "qwen3.5-plus");

        assertEquals(Boolean.TRUE, result.memoryCompressed());
        assertEquals(6400, result.tokenCountBefore());
        assertEquals(700, result.tokenCountAfter());
        assertEquals(3, result.compressedMessageCount());
        assertEquals(1, result.keptRecentCount());
        assertEquals("## Execution State\n- Task: compressed", result.summary());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(aiMessageService).replaceConversationMessages(eq(conversationId), captor.capture());
        verify(aiConversationService).updateTokenCount(conversationId, 700);
        assertEquals(2, captor.getValue().size());
        assertTrue(ChatMemoryCompressor.isSummaryMessage(captor.getValue().get(1)));
        assertTrue(compressor.consumeDoneMetadata(conversationId).isEmpty());
<<<<<<< HEAD
=======

        ArgumentCaptor<AgentLogEvent> logCaptor = ArgumentCaptor.forClass(AgentLogEvent.class);
        verify(agentLogService, atLeastOnce()).record(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream().anyMatch(event ->
                CompressionLogConstant.EVENT_COMPRESSION_STARTED.equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())));
        assertTrue(logCaptor.getAllValues().stream().anyMatch(event ->
                CompressionLogConstant.EVENT_COMPRESSION_COMPLETED.equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())
                        && Integer.valueOf(700).equals(event.getPayload().get("tokenCountAfter"))));
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    @Test
    void compressNow_withTooFewMessages_recordsSkippedEvent() {
        Long conversationId = 613L;
        when(aiConversationService.getByIdForCurrentUser(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .tokenCount(900)
                .build());
        when(aiMessageService.getActiveByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(
                stored(conversationId, UserMessage.from("u1")),
                stored(conversationId, UserMessage.from("u2")),
                stored(conversationId, UserMessage.from("u3"))
        ));

        CompressionDoneMetadata result = compressor.compressNow(conversationId, "qwen3.5-plus");

        assertEquals(Boolean.FALSE, result.memoryCompressed());
        assertEquals(900, result.tokenCountBefore());
        assertEquals(900, result.tokenCountAfter());
        assertEquals(null, result.summary());
<<<<<<< HEAD
=======

        ArgumentCaptor<AgentLogEvent> logCaptor = ArgumentCaptor.forClass(AgentLogEvent.class);
        verify(agentLogService, atLeastOnce()).record(logCaptor.capture());
        assertTrue(logCaptor.getAllValues().stream().anyMatch(event ->
                CompressionLogConstant.EVENT_COMPRESSION_SKIPPED.equals(event.getMessage())
                        && conversationId.equals(event.getConversationId())
                        && CompressionLogConstant.DECISION_SKIP_NOT_ENOUGH_MESSAGES.equals(event.getPayload().get("decision"))));
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        verify(aiMessageService, never()).replaceConversationMessages(any(), any());
    }

    @Test
    void compressNow_whenCompressionFails_recordsFailedEvent() {
        Long conversationId = 714L;
        when(aiConversationService.getByIdForCurrentUser(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .tokenCount(4200)
                .build());
        when(aiMessageService.getActiveByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(
                stored(conversationId, UserMessage.from("u1")),
                stored(conversationId, UserMessage.from("u2")),
                stored(conversationId, UserMessage.from("u3")),
                stored(conversationId, UserMessage.from("u4"))
        ));
        when(compressionService.compress(any())).thenThrow(new IllegalStateException("boom"));

        CompressionDoneMetadata result = compressor.compressNow(conversationId, "qwen3.5-plus");

        assertEquals(Boolean.FALSE, result.memoryCompressed());
        assertEquals(4200, result.tokenCountBefore());
        assertEquals(4200, result.tokenCountAfter());
        assertEquals(null, result.summary());
<<<<<<< HEAD
=======

        verify(agentLogService).recordError(
                eq(AgentLogType.DEBUG_EVENT),
                eq(CompressionLogConstant.LOGGER_NAME),
                eq(CompressionLogConstant.EVENT_COMPRESSION_FAILED),
                any(IllegalStateException.class),
                anyMap()
        );
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        verify(aiMessageService, never()).replaceConversationMessages(any(), any());
    }

    private StoredChatMessage stored(Long conversationId, ChatMessage message) {
        return StoredChatMessage.builder()
                .conversationId(conversationId)
                .status(edu.zsc.ai.common.enums.ai.MessageStatusEnum.NORMAL.getCode())
                .data(ChatMessageSerializer.messageToJson(message))
                .build();
    }
}
