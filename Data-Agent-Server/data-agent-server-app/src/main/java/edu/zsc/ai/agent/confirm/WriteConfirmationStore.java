package edu.zsc.ai.agent.confirm;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory store for write confirmation tokens, backed by Caffeine.
 * Entries expire automatically after 5 minutes; no manual cleanup needed.
 *
 * Security guarantees:
 * - userId + conversationId binding prevents cross-user and cross-session attacks.
 * - CONSUMED state prevents token replay.
 * - TTL is enforced by Caffeine expiry.
 */
@Component
@Slf4j
public class WriteConfirmationStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Cache<String, WriteConfirmationEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(10_000)
            .build();

    /**
     * Create a new PENDING confirmation token bound to the given user and conversation.
     */
    public WriteConfirmationEntry create(Long userId, Long conversationId,
                                         String sql, String databaseName, String schemaName) {
        String token = UUID.randomUUID().toString();
        WriteConfirmationEntry entry = WriteConfirmationEntry.builder()
                .token(token)
                .userId(userId)
                .conversationId(conversationId)
                .sql(sql)
                .databaseName(databaseName)
                .schemaName(schemaName)
                .status(WriteConfirmationStatus.PENDING)
                .build();
        cache.put(token, entry);
        log.info("[WriteConfirm] Created token={} userId={} conversationId={}", token, userId, conversationId);
        return entry;
    }

    /**
     * Transition a PENDING token to CONFIRMED.
     * Verifies that the token belongs to this userId.
     *
     * @return true if successfully confirmed, false otherwise
     */
    public boolean confirm(String token, Long userId) {
        WriteConfirmationEntry entry = cache.getIfPresent(token);
        if (entry == null) {
            log.warn("[WriteConfirm] confirm: token not found or expired token={}", token);
            return false;
        }
        synchronized (entry) {
            if (!entry.getUserId().equals(userId)) {
                log.warn("[WriteConfirm] confirm: ownership mismatch token={} expectedUser={} actualUser={}",
                        token, entry.getUserId(), userId);
                return false;
            }
            if (entry.getStatus() != WriteConfirmationStatus.PENDING) {
                log.warn("[WriteConfirm] confirm: not PENDING token={} status={}", token, entry.getStatus());
                return false;
            }
            entry.setStatus(WriteConfirmationStatus.CONFIRMED);
            log.info("[WriteConfirm] Confirmed token={}", token);
            return true;
        }
    }

    /**
     * Invalidate a PENDING token (user cancelled).
     * Removes it from the cache so it can never be confirmed or consumed.
     *
     * @return true if found and removed, false if not found or not owned by this user
     */
    public boolean cancel(String token, Long userId) {
        WriteConfirmationEntry entry = cache.getIfPresent(token);
        if (entry == null) {
            log.warn("[WriteConfirm] cancel: token not found or expired token={}", token);
            return false;
        }
        if (!entry.getUserId().equals(userId)) {
            log.warn("[WriteConfirm] cancel: ownership mismatch token={} expectedUser={} actualUser={}",
                    token, entry.getUserId(), userId);
            return false;
        }
        cache.invalidate(token);
        log.info("[WriteConfirm] Cancelled token={}", token);
        return true;
    }

    /**
     * Find a CONFIRMED token matching userId + conversationId + sql, then consume it.
     * The agent never needs to know or pass the token — the server matches it automatically.
     *
     * @return true if a matching CONFIRMED token was found and consumed, false otherwise
     */
    public boolean consumeConfirmedBySql(Long userId, Long conversationId, String sql) {
        return cache.asMap().values().stream()
                .filter(e -> e.getUserId().equals(userId)
                        && e.getConversationId().equals(conversationId)
                        && e.getStatus() == WriteConfirmationStatus.CONFIRMED
                        && e.getSql().equals(sql))
                .findFirst()
                .map(entry -> {
                    synchronized (entry) {
                        // Double-check after acquiring lock to handle concurrent calls
                        if (entry.getStatus() != WriteConfirmationStatus.CONFIRMED
                                || !entry.getSql().equals(sql)) {
                            return false;
                        }
                        entry.setStatus(WriteConfirmationStatus.CONSUMED);
                        log.info("[WriteConfirm] Consumed by sql match: userId={} conversationId={}",
                                userId, conversationId);
                        return true;
                    }
                })
                .orElseGet(() -> {
                    log.warn("[WriteConfirm] consumeConfirmedBySql: no CONFIRMED token found for userId={} conversationId={}",
                            userId, conversationId);
                    return false;
                });
    }
}
