package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationMemoryCursorService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    private ConversationMemoryWriter memoryWriter;

    @Mock
    private AiConversationMemoryCursorService cursorService;

    @Mock
    private AiConversationService aiConversationService;

    @Mock
    private ConversationMemoryAdvisoryLockService advisoryLockService;

    @InjectMocks
    private ConversationMemoryAutoWriteCoordinator coordinator;

    @BeforeEach
    void stubAdvisoryLock() {
        lenient().when(advisoryLockService.tryRunWithConversationLock(anyLong(), any()))
                .thenAnswer(invocation -> {
                    invocation.getArgument(1, Runnable.class).run();
                    return true;
                });
    }

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

        verify(memoryWriter, never()).writeMemory(any(MemoryWriteContext.class), anyLong(), anyLong());
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

        verify(memoryWriter, never()).writeMemory(any(MemoryWriteContext.class), anyLong(), anyLong());
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

        verify(memoryWriter, never()).writeMemory(any(MemoryWriteContext.class), anyLong(), anyLong());
        ArgumentCaptor<AiConversationMemoryCursor> cursorCaptor = ArgumentCaptor.forClass(AiConversationMemoryCursor.class);
        verify(cursorService).updateById((AiConversationMemoryCursor) cursorCaptor.capture());
        assertEquals(22L, cursorCaptor.getValue().getLastProcessedMessageId());
    }

    @Test
    void submit_buildsSliceContextAndInvokesMemoryWriter() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        List<StoredChatMessage> newMessages = List.of(
                message(101L, "USER", "以后默认用中文回答"),
                message(102L, "AI", "收到")
        );

        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(newMessages);
        doNothing().when(memoryWriter).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        coordinator.submit(88L);

        ArgumentCaptor<MemoryWriteContext> contextCaptor = ArgumentCaptor.forClass(MemoryWriteContext.class);
        verify(memoryWriter).writeMemory(contextCaptor.capture(), eq(88L), eq(7L));
        MemoryWriteContext context = contextCaptor.getValue();
        assertEquals(2, context.newMessages().size());
        assertTrue(context.existingMemories().isEmpty());
        verify(cursorService).updateById(any(AiConversationMemoryCursor.class));
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
        doNothing().when(memoryWriter).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        // First call passes (counter starts at EXTRACTION_INTERVAL)
        coordinator.submit(88L);
        verify(memoryWriter, times(1)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        // Second call is throttled (counter reset to 0, 0 < 1 = true)
        coordinator.submit(88L);
        verify(memoryWriter, times(1)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        // Third call passes again
        coordinator.submit(88L);
        verify(memoryWriter, times(2)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));
    }

    @Test
    void submit_doesNotAdvanceCursorWhenMemoryWriterFails() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "remember this")
        ));
        doThrow(new RuntimeException("write failed"))
                .when(memoryWriter).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        coordinator.submit(88L);

        verify(memoryWriter, times(3)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));
        verify(cursorService, never()).updateById((AiConversationMemoryCursor) any());
    }

    @Test
    void submit_marksPendingAndRunsOneFollowUpAfterCurrentRun() throws Exception {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        CountDownLatch firstCallEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstCall = new CountDownLatch(1);
        AtomicInteger writerCalls = new AtomicInteger();

        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "first")
        ));
        when(aiMessageService.getMessagesForAutoWriteAfter(88L, 1L)).thenReturn(List.of(
                message(2L, "USER", "second")
        ));
        doAnswer(invocation -> {
            if (writerCalls.incrementAndGet() == 1) {
                firstCallEntered.countDown();
                assertTrue(releaseFirstCall.await(2, TimeUnit.SECONDS));
            }
            return null;
        }).when(memoryWriter).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

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

        verify(memoryWriter, times(2)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void submit_retriesMemoryWriterUntilSuccess() {
        AiConversationMemoryCursor cursor = cursor(88L, 7L, null);
        givenConversationAndCursor(88L, 7L, cursor);
        when(memoryService.hasManualWritesSince(eq(7L), eq(88L), any())).thenReturn(false);
        when(aiMessageService.getActiveMessagesForAutoWrite(88L)).thenReturn(List.of(
                message(1L, "USER", "remember")
        ));
        doThrow(new RuntimeException("first failure"))
                .doNothing()
                .when(memoryWriter).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));

        coordinator.submit(88L);

        verify(memoryWriter, times(2)).writeMemory(any(MemoryWriteContext.class), eq(88L), eq(7L));
        verify(cursorService).updateById(any());
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
}
