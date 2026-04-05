package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationMemoryCursorService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext.MemorySummary;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryAutoWriteCoordinatorTest {

    @Mock
    private AiMessageService aiMessageService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private ConversationMemoryAiWriter aiWriter;

    @Mock
    private AiConversationMemoryCursorService cursorService;

    @Mock
    private AiConversationService aiConversationService;

    @InjectMocks
    private ConversationMemoryAutoWriteCoordinator coordinator;

    @Test
    void submit_advancesCursorWhenManualMemoryWriteExists() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, 9L);
        cursor.setLastProcessedAt(LocalDateTime.of(2026, 3, 31, 8, 0));
        givenConversationAndCursor(88L, 7L, cursor);
        when(aiMessageService.getMessagesForAutoWriteAfter(88L, 9L)).thenReturn(List.of(
                message(10L, "USER", "hello"),
                message(11L, "AI", "hi")
        ));
        when(memoryService.hasManualWritesSince(7L, 88L, cursor.getLastProcessedAt())).thenReturn(true);

        // First call passes (counter initialized at EXTRACTION_INTERVAL)
        coordinator.submit(88L);

        verify(aiWriter, never()).extractMemoryWrites(any(MemoryWriteContext.class));
        ArgumentCaptor<AiConversationMemoryCursor> cursorCaptor = ArgumentCaptor.forClass(AiConversationMemoryCursor.class);
        verify(cursorService).updateById((AiConversationMemoryCursor) cursorCaptor.capture());
        assertEquals(11L, cursorCaptor.getValue().getLastProcessedMessageId());
    }

    @Test
    void submit_skipsWithoutCursorUpdateWhenNoNewMessages() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, 9L);
        givenConversationAndCursor(88L, 7L, cursor);
        when(aiMessageService.getMessagesForAutoWriteAfter(88L, 9L)).thenReturn(List.of());

        coordinator.submit(88L);

        verify(aiWriter, never()).extractMemoryWrites(any(MemoryWriteContext.class));
        verify(cursorService, never()).updateById((AiConversationMemoryCursor) any());
    }

    @Test
    void submit_advancesCursorWithoutAiCallWhenSliceHasNoUserMessages() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        givenConversationAndCursor(88L, 7L, cursor);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(21L, "AI", "我先去查一下"),
                message(22L, "TOOL_EXECUTION_RESULT", "{\"rows\": 10}")
        ));

        coordinator.submit(88L);

        verify(aiWriter, never()).extractMemoryWrites(any(MemoryWriteContext.class));
        ArgumentCaptor<AiConversationMemoryCursor> cursorCaptor = ArgumentCaptor.forClass(AiConversationMemoryCursor.class);
        verify(cursorService).updateById((AiConversationMemoryCursor) cursorCaptor.capture());
        assertEquals(22L, cursorCaptor.getValue().getLastProcessedMessageId());
    }

    @Test
    void submit_buildsTruncatedContextAndAppliesReturnedItems() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        List<MemorySummary> summaries = new ArrayList<>();
        for (long i = 1; i <= 60; i++) {
            summaries.add(summary(i));
        }
        List<StoredChatMessage> newMessages = List.of(
                message(101L, "USER", "以后默认用中文回答"),
                message(102L, "AI", "收到")
        );
        List<MemoryWriteItem> writeItems = List.of(
                new MemoryWriteItem("CREATE", null, "USER", "PREFERENCE",
                        "LANGUAGE_PREFERENCE", "语言偏好", "默认使用中文回答", "用户明确提出偏好")
        );

        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(newMessages);
        when(memoryService.getEnabledMemorySummaries(7L)).thenReturn(summaries);
        when(aiWriter.extractMemoryWrites(any(MemoryWriteContext.class))).thenReturn(writeItems);

        coordinator.submit(88L);

        ArgumentCaptor<MemoryWriteContext> contextCaptor = ArgumentCaptor.forClass(MemoryWriteContext.class);
        verify(aiWriter).extractMemoryWrites(contextCaptor.capture());
        MemoryWriteContext context = contextCaptor.getValue();
        assertEquals(2, context.newMessages().size());
        assertEquals(50, context.existingMemories().size());
        assertTrue(context.memoriesTruncated());
        assertEquals(60, context.totalEnabledCount());
        assertEquals(1L, context.existingMemories().get(0).memoryId());
        assertEquals(50L, context.existingMemories().get(49).memoryId());

        verify(memoryService).applyAutoWriteItems(eq(88L), eq(7L), eq(writeItems), eq(cursor), eq(102L));
    }

    @Test
    void submit_throttlesSecondCallAndExecutesThird() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "hello")
        ));
        // After first run advances cursor to 1L, subsequent calls use getMessagesForAutoWriteAfter
        when(aiMessageService.getMessagesForAutoWriteAfter(eq(88L), any())).thenReturn(List.of(
                message(2L, "USER", "world")
        ));
        when(memoryService.getEnabledMemorySummaries(7L)).thenReturn(List.of());
        when(aiWriter.extractMemoryWrites(any(MemoryWriteContext.class))).thenReturn(List.of());

        // First call passes (counter starts at EXTRACTION_INTERVAL)
        coordinator.submit(88L);
        verify(aiWriter, times(1)).extractMemoryWrites(any(MemoryWriteContext.class));

        // Second call is throttled (counter reset to 0, 0 < 1 = true)
        coordinator.submit(88L);
        verify(aiWriter, times(1)).extractMemoryWrites(any(MemoryWriteContext.class));

        // Third call passes again
        coordinator.submit(88L);
        verify(aiWriter, times(2)).extractMemoryWrites(any(MemoryWriteContext.class));
    }

    @Test
    void submit_doesNotAdvanceCursorWhenAiExtractionFails() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "remember this")
        ));
        when(memoryService.getEnabledMemorySummaries(7L)).thenReturn(List.of());
        // null signals AI failure
        when(aiWriter.extractMemoryWrites(any(MemoryWriteContext.class))).thenReturn(null);

        coordinator.submit(88L);

        verify(cursorService, never()).updateById((AiConversationMemoryCursor) any());
        verify(memoryService, never()).applyAutoWriteItems(any(), any(), any(), any(), any());
    }

    @Test
    void submit_marksPendingAndRunsOneFollowUpAfterCurrentRun() throws Exception {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        CountDownLatch firstCallEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstCall = new CountDownLatch(1);
        AtomicInteger writerCalls = new AtomicInteger();

        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(memoryService.getEnabledMemorySummaries(7L)).thenReturn(List.of());
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "first")
        ));
        when(aiMessageService.getMessagesForAutoWriteAfter(88L, 1L)).thenReturn(List.of(
                message(2L, "USER", "second")
        ));
        when(aiWriter.extractMemoryWrites(any(MemoryWriteContext.class))).thenAnswer(invocation -> {
            if (writerCalls.incrementAndGet() == 1) {
                firstCallEntered.countDown();
                assertTrue(releaseFirstCall.await(2, TimeUnit.SECONDS));
            }
            return List.of();
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // First call passes throttle (counter starts at EXTRACTION_INTERVAL)
            Future<?> firstRun = executor.submit(() -> coordinator.submit(88L));
            assertTrue(firstCallEntered.await(2, TimeUnit.SECONDS));

            // Second call while first is running: passes throttle but CAS fails → marks pending
            // The pending follow-up bypasses throttle via executeWithPendingCheck
            coordinator.submit(88L); // throttled
            coordinator.submit(88L); // CAS fails on inFlight → marks pending
            releaseFirstCall.countDown();
            firstRun.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        verify(aiWriter, times(2)).extractMemoryWrites(any(MemoryWriteContext.class));
        assertFalse(Thread.currentThread().isInterrupted());
    }

    private void givenConversationAndCursor(Long conversationId, Long userId, AiConversationMemoryCursor cursor) {
        when(aiConversationService.getById(conversationId)).thenReturn(AiConversation.builder()
                .id(conversationId)
                .userId(userId)
                .build());
        when(cursorService.getOne(any(), eq(false))).thenReturn(cursor);
    }

    private AiConversationMemoryCursor cursor(Long conversationId, Long userId, Long lastProcessedMessageId) {
        return AiConversationMemoryCursor.builder()
                .conversationId(conversationId)
                .userId(userId)
                .lastProcessedMessageId(lastProcessedMessageId)
                .createdAt(LocalDateTime.of(2026, 3, 30, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 30, 8, 0))
                .build();
    }

    private StoredChatMessage message(Long id, String role, String data) {
        return StoredChatMessage.builder()
                .id(id)
                .role(role)
                .data(data)
                .build();
    }

    private MemorySummary summary(Long memoryId) {
        return new MemorySummary(
                memoryId,
                "USER",
                "PREFERENCE",
                "LANGUAGE_PREFERENCE",
                "title-" + memoryId,
                "content-" + memoryId,
                LocalDateTime.of(2026, 3, 1, 0, 0)
        );
    }
}
