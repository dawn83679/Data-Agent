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
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext.MemorySummary;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
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

    private static final int MAX_MEMORY_SUMMARIES = 50;
    private static final int MAX_MESSAGES_PER_EXTRACTION = 30;
    private static final int EXTRACTION_INTERVAL = 1;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final AiMessageService aiMessageService;
    private final MemoryService memoryService;
    private final ConversationMemoryAiWriter aiWriter;
    private final AiConversationMemoryCursorService cursorService;
    private final AiConversationService aiConversationService;

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
            doWrite(conversationId);
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
            MemoryWriteContext context = buildWriteContext(userId, newMessages);
            Long processedUpTo = context.lastMessageId();
            log.info("[MemAutoWrite] Context built: conversationId={}, messagesForAI={}, existingMemories={}, truncated={}, processedUpTo={}",
                    conversationId, context.newMessages().size(), context.existingMemories().size(),
                    context.memoriesTruncated(), processedUpTo);

            log.info("[MemAutoWrite] Calling AI for memory extraction: conversationId={}", conversationId);
            long aiStartTime = System.currentTimeMillis();
            List<MemoryWriteItem> items = aiWriter.extractMemoryWrites(context);
            long aiElapsed = System.currentTimeMillis() - aiStartTime;

            if (items == null) {
                log.error("[MemAutoWrite] AI extraction returned null (parse failure or provider error): conversationId={}, aiElapsedMs={}",
                        conversationId, aiElapsed);
                throw new RuntimeException("AI extraction returned null (provider error or parse failure)");
            }

            if (!items.isEmpty()) {
                log.info("[MemAutoWrite] Extracted {} memory items in {}ms: conversationId={}", items.size(), aiElapsed, conversationId);
                for (int i = 0; i < items.size(); i++) {
                    MemoryWriteItem item = items.get(i);
                    log.info("[MemAutoWrite]   item[{}]: op={}, memoryId={}, type={}/{}, title={}",
                            i, item.operation(), item.memoryId(), item.memoryType(), item.subType(),
                            item.title() != null ? item.title() : "(null)");
                }
                memoryService.applyAutoWriteItems(conversationId, userId, items, cursor, processedUpTo);
                log.info("[MemAutoWrite] Applied {} items and advanced cursor: conversationId={}, cursorTo={}",
                        items.size(), conversationId, processedUpTo);
            } else {
                log.info("[MemAutoWrite] AI returned empty items (nothing to remember) in {}ms: conversationId={}", aiElapsed, conversationId);
                advanceCursor(cursor, processedUpTo);
            }

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

    private MemoryWriteContext buildWriteContext(Long userId, List<StoredChatMessage> newMessages) {
        // Cap messages to avoid oversized prompts on first-time or catch-up extractions
        if (newMessages.size() > MAX_MESSAGES_PER_EXTRACTION) {
            newMessages = newMessages.subList(
                    newMessages.size() - MAX_MESSAGES_PER_EXTRACTION, newMessages.size());
        }

        List<MemorySummary> summaries = memoryService.getEnabledMemorySummaries(userId);
        boolean truncated = summaries.size() > MAX_MEMORY_SUMMARIES;
        int total = summaries.size();
        if (truncated) {
            summaries = summaries.subList(0, MAX_MEMORY_SUMMARIES);
        }
        return new MemoryWriteContext(newMessages, summaries, truncated, total);
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
        log.warn("[MemAutoWrite] Failure #{} for conversationId={} (forceAdvanceAt={})",
                failures, conversationId, MAX_CONSECUTIVE_FAILURES);
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
