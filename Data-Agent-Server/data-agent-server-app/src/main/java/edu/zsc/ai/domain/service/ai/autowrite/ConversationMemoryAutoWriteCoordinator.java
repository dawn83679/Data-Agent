package edu.zsc.ai.domain.service.ai.autowrite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationMemoryCursorService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates per-conversation auto memory writes with serialization
 * (at most one in-flight + one pending per conversation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemoryAutoWriteCoordinator {

    private static final int MAX_MESSAGES_PER_EXTRACTION = 30;
    /**
     * Number of completed chat turns to skip between two extractions. Counter is initialized to this value
     * so the first {@link #submit} after a cold start runs immediately; each subsequent run resets the
     * counter to 0, meaning one throttled submit per extraction (interval=1 → every other completion).
     */
    private static final int EXTRACTION_INTERVAL = 1;
    /** After this many consecutive failures, cursor is advanced to avoid blocking the pipeline. */
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final int MAX_MEMORY_WRITER_ATTEMPTS = 3;

    private final AiMessageService aiMessageService;
    private final MemoryService memoryService;
    private final ConversationMemoryWriter memoryWriter;
    private final AiConversationMemoryCursorService cursorService;
    private final AiConversationService aiConversationService;
    private final ConversationMemoryAdvisoryLockService advisoryLockService;

    private final ConcurrentHashMap<Long, AtomicBoolean> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicBoolean> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> turnCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> failureCounts = new ConcurrentHashMap<>();

    public void submit(Long conversationId) {
        // Throttle: execute on first call, then skip EXTRACTION_INTERVAL turns before next extraction.
        // Counter tracks turns since last extraction (or init). Starts at EXTRACTION_INTERVAL so first call passes.
        AtomicInteger turnsSince = turnCounters.computeIfAbsent(conversationId, k -> new AtomicInteger(EXTRACTION_INTERVAL));
        if (turnsSince.incrementAndGet() <= EXTRACTION_INTERVAL) {
            log.info("[MemAutoWrite] Throttled: conversationId={}, turn={}/{}", conversationId, turnsSince.get(), EXTRACTION_INTERVAL);
            return;
        }
        turnsSince.set(0);
        log.info("[MemAutoWrite] Throttle passed, starting extraction: conversationId={}", conversationId);

        executeWithPendingCheck(conversationId);
    }

    private void executeWithPendingCheck(Long conversationId) {
        AtomicBoolean running = inFlight.computeIfAbsent(conversationId, k -> new AtomicBoolean(false));

        if (!running.compareAndSet(false, true)) {
            pending.computeIfAbsent(conversationId, k -> new AtomicBoolean(false)).set(true);
            log.info("[MemAutoWrite] Already in-flight, marked pending: conversationId={}", conversationId);
            return;
        }

        try {
            boolean ran = advisoryLockService.tryRunWithConversationLock(conversationId, () -> doWrite(conversationId));
            if (!ran) {
                log.info("[MemAutoWrite] Extraction skipped (advisory lock not acquired): conversationId={}",
                        conversationId);
            }
        } finally {
            running.set(false);
        }

        // Check if a follow-up run is needed (bypasses throttle — already queued)
        AtomicBoolean pendingFlag = pending.get(conversationId);
        if (pendingFlag != null && pendingFlag.compareAndSet(true, false)) {
            log.info("[MemAutoWrite] Running pending follow-up: conversationId={}", conversationId);
            executeWithPendingCheck(conversationId);
        }
    }

    private void doWrite(Long conversationId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[MemAutoWrite] --- doWrite START --- conversationId={}", conversationId);

            AiConversation conversation = aiConversationService.getById(conversationId);
            if (conversation == null) {
                log.warn("[MemAutoWrite] Conversation not found, skipping: conversationId={}", conversationId);
                return;
            }
            Long userId = conversation.getUserId();
            log.info("[MemAutoWrite] Loaded conversation: conversationId={}, userId={}", conversationId, userId);

            AiConversationMemoryCursor cursor = getOrCreateCursor(conversationId, userId);
            log.info("[MemAutoWrite] Cursor state: conversationId={}, lastProcessedMessageId={}, lastProcessedAt={}",
                    conversationId, cursor.getLastProcessedMessageId(), cursor.getLastProcessedAt());

            List<StoredChatMessage> newMessages = fetchNewMessages(conversationId, cursor);
            log.info("[MemAutoWrite] Fetched new messages: conversationId={}, count={}", conversationId, newMessages.size());

            if (newMessages.isEmpty()) {
                log.info("[MemAutoWrite] No new messages, skipping: conversationId={}", conversationId);
                return;
            }

            Long lastMessageId = newMessages.get(newMessages.size() - 1).getId();
            log.debug("[MemAutoWrite] Message range: firstId={}, lastId={}", newMessages.get(0).getId(), lastMessageId);

            if (memoryService.hasManualWritesSince(userId, conversationId, cursor.getLastProcessedAt())) {
                log.info("[MemAutoWrite] Manual write detected, advancing cursor without extraction: conversationId={}, cursorTo={}",
                        conversationId, lastMessageId);
                advanceCursor(cursor, lastMessageId);
                return;
            }

            boolean hasUserMessage = newMessages.stream()
                    .anyMatch(m -> "USER".equals(m.getRole()));
            if (!hasUserMessage) {
                log.info("[MemAutoWrite] No USER messages in slice, advancing cursor: conversationId={}, cursorTo={}",
                        conversationId, lastMessageId);
                advanceCursor(cursor, lastMessageId);
                return;
            }

            // Build context and call AI
            MemoryWriteContext context = buildWriteContext(newMessages);
            Long processedUpTo = context.lastMessageId();
            log.info("[MemAutoWrite] Context built: conversationId={}, messagesForAI={}, existingMemories={}, truncated={}, processedUpTo={}",
                    conversationId, context.newMessages().size(), context.existingMemories().size(),
                    context.memoriesTruncated(), processedUpTo);

            log.info("[MemAutoWrite] Calling memory writer agent: conversationId={}", conversationId);
            long writerStartTime = System.currentTimeMillis();
            Exception lastFailure = null;
            boolean writerSucceeded = false;
            for (int attempt = 1; attempt <= MAX_MEMORY_WRITER_ATTEMPTS; attempt++) {
                try {
                    memoryWriter.writeMemory(context, conversationId, userId);
                    writerSucceeded = true;
                    break;
                } catch (Exception e) {
                    lastFailure = e;
                    log.warn("[MemAutoWrite] Memory writer failed (attempt {}/{}): conversationId={}, error={}",
                            attempt, MAX_MEMORY_WRITER_ATTEMPTS, conversationId, e.getMessage());
                }
                if (attempt < MAX_MEMORY_WRITER_ATTEMPTS) {
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
            long writerElapsed = System.currentTimeMillis() - writerStartTime;

            if (!writerSucceeded) {
                log.error("[MemAutoWrite] Memory writer failed after {} attempts: conversationId={}, elapsedMs={}",
                        MAX_MEMORY_WRITER_ATTEMPTS, conversationId, writerElapsed, lastFailure);
                throw new RuntimeException("Memory writer failed", lastFailure);
            }

            advanceCursor(cursor, processedUpTo);
            log.info("[MemAutoWrite] Memory writer completed in {}ms and advanced cursor: conversationId={}, cursorTo={}",
                    writerElapsed, conversationId, processedUpTo);

            resetFailureCount(conversationId);
            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("[MemAutoWrite] --- doWrite SUCCESS --- conversationId={}, totalElapsedMs={}", conversationId, totalElapsed);
        } catch (Exception e) {
            long totalElapsed = System.currentTimeMillis() - startTime;
            log.error("[MemAutoWrite] --- doWrite FAILED --- conversationId={}, totalElapsedMs={}", conversationId, totalElapsed, e);
            handleFailure(conversationId);
        }
    }

    private List<StoredChatMessage> fetchNewMessages(Long conversationId, AiConversationMemoryCursor cursor) {
        if (cursor.getLastProcessedMessageId() != null) {
            return aiMessageService.getMessagesForAutoWriteAfter(
                    conversationId, cursor.getLastProcessedMessageId());
        }
        return aiMessageService.getActiveMessagesForAutoWrite(conversationId);
    }

    private MemoryWriteContext buildWriteContext(List<StoredChatMessage> newMessages) {
        // Cap messages to avoid oversized prompts on first-time or catch-up extractions
        if (newMessages.size() > MAX_MESSAGES_PER_EXTRACTION) {
            newMessages = newMessages.subList(
                    newMessages.size() - MAX_MESSAGES_PER_EXTRACTION, newMessages.size());
        }
        return new MemoryWriteContext(newMessages, List.of(), false, 0);
    }

    private AiConversationMemoryCursor getOrCreateCursor(Long conversationId, Long userId) {
        LambdaQueryWrapper<AiConversationMemoryCursor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversationMemoryCursor::getConversationId, conversationId);
        AiConversationMemoryCursor cursor = cursorService.getOne(wrapper, false);

        if (cursor == null) {
            cursor = AiConversationMemoryCursor.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            cursorService.save(cursor);
        }
        return cursor;
    }

    private void advanceCursor(AiConversationMemoryCursor cursor, Long lastMessageId) {
        cursor.setLastProcessedMessageId(lastMessageId);
        cursor.setLastProcessedAt(LocalDateTime.now());
        cursor.setUpdatedAt(LocalDateTime.now());
        cursorService.updateById(cursor);
    }

    private void resetFailureCount(Long conversationId) {
        AtomicInteger count = failureCounts.get(conversationId);
        if (count != null) {
            count.set(0);
        }
    }

    private void handleFailure(Long conversationId) {
        AtomicInteger count = failureCounts.computeIfAbsent(conversationId, k -> new AtomicInteger(0));
        int failures = count.incrementAndGet();
        log.warn(
                "[MemAutoWrite] extraction_failure conversationId={} consecutiveFailures={} forceAdvanceThreshold={} failureStage=doWrite",
                conversationId, failures, MAX_CONSECUTIVE_FAILURES);
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("[MemAutoWrite] Force-advancing cursor after {} consecutive failures: conversationId={}",
                    failures, conversationId);
            try {
                AiConversation conversation = aiConversationService.getById(conversationId);
                if (conversation != null) {
                    AiConversationMemoryCursor cursor = getOrCreateCursor(conversationId, conversation.getUserId());
                    List<StoredChatMessage> messages = fetchNewMessages(conversationId, cursor);
                    if (!messages.isEmpty()) {
                        Long advanceTo = messages.get(messages.size() - 1).getId();
                        advanceCursor(cursor, advanceTo);
                        log.info("[MemAutoWrite] Force-advanced cursor: conversationId={}, cursorTo={}", conversationId, advanceTo);
                    }
                }
            } catch (Exception ex) {
                log.error("[MemAutoWrite] Failed to force-advance cursor: conversationId={}", conversationId, ex);
            }
            count.set(0);
        }
    }
}
