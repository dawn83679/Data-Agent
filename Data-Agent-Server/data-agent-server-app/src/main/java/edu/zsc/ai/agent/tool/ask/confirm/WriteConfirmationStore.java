package edu.zsc.ai.agent.tool.ask.confirm;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.context.DbContext;
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
     * Create a new PENDING confirmation token bound to the current user and conversation context.
     */
    public WriteConfirmationEntry create(DbContext db, String sql) {
        Long userId = RequestContext.getUserId();
        Long conversationId = RequestContext.getConversationId();

        String token = UUID.randomUUID().toString();
        WriteConfirmationEntry entry = WriteConfirmationEntry.builder()
                .token(token)
                .userId(userId)
                .conversationId(conversationId)
                .connectionId(db.connectionId())
                .sql(sql)
                .catalog(db.catalog())
                .schema(db.schema())
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
    public boolean confirm(String token) {
        Long userId = RequestContext.getUserId();
        WriteConfirmationEntry entry = cache.getIfPresent(token);
        if (entry == null || !entry.getUserId().equals(userId) || entry.getStatus() != WriteConfirmationStatus.PENDING) {
            log.warn("[WriteConfirm] confirm failed: invalid token, ownership mismatch, or not PENDING. token={}", token);
            return false;
        }
        entry.setStatus(WriteConfirmationStatus.CONFIRMED);
        log.info("[WriteConfirm] Confirmed token={}", token);
        return true;
    }

    /**
     * Invalidate a PENDING token (user cancelled).
     * Removes it from the cache so it can never be confirmed or consumed.
     *
     * @return true if found and removed, false if not found or not owned by this user
     */
    public boolean cancel(String token) {
        Long userId = RequestContext.getUserId();
        WriteConfirmationEntry entry = cache.getIfPresent(token);
        if (entry == null || !entry.getUserId().equals(userId)) {
            log.warn("[WriteConfirm] cancel failed: token not found or ownership mismatch. token={}", token);
            return false;
        }
        cache.invalidate(token);
        log.info("[WriteConfirm] Cancelled token={}", token);
        return true;
    }

    /**
     * Find a CONFIRMED token matching current userId + conversationId + connection + catalog + schema + sql, then consume it.
     * The agent never needs to know or pass the token — the server matches it automatically.
     *
     * @return a {@link WriteConsumeResult} indicating success or a structured failure reason
     */
    public WriteConsumeResult consumeConfirmedBySql(DbContext db, String sql) {
        Long userId = RequestContext.getUserId();
        Long conversationId = RequestContext.getConversationId();
        String normalizedSql = normalizeSql(sql);

        // Step 1: find all tokens for this user + conversation
        List<WriteConfirmationEntry> sessionTokens = cache.asMap().values().stream()
                .filter(e -> e.getUserId().equals(userId) && e.getConversationId().equals(conversationId))
                .toList();

        if (sessionTokens.isEmpty()) {
            log.warn("[WriteConfirm] consumeConfirmedBySql: no tokens at all for userId={} conversationId={}", userId, conversationId);
            return WriteConsumeResult.fail("NO_TOKEN",
                    "No confirmation token exists for this conversation. You must call askUserConfirm first.");
        }

        // Step 2: check token statuses
        boolean hasConfirmed = sessionTokens.stream().anyMatch(e -> e.getStatus() == WriteConfirmationStatus.CONFIRMED);

        if (!hasConfirmed) {
            boolean hasPending = sessionTokens.stream().anyMatch(e -> e.getStatus() == WriteConfirmationStatus.PENDING);
            if (hasPending) {
                log.warn("[WriteConfirm] consumeConfirmedBySql: only PENDING tokens for userId={} conversationId={}", userId, conversationId);
                return WriteConsumeResult.fail("NOT_CONFIRMED",
                        "Confirmation token exists but user has not confirmed yet. Wait for user confirmation.");
            }
            // all must be CONSUMED
            log.warn("[WriteConfirm] consumeConfirmedBySql: only CONSUMED tokens for userId={} conversationId={}", userId, conversationId);
            return WriteConsumeResult.fail("ALREADY_CONSUMED",
                    "Confirmation token was already used. Call askUserConfirm again for a new confirmation.");
        }

        // Step 3: among CONFIRMED tokens, try to find an exact match
        List<WriteConfirmationEntry> confirmedTokens = sessionTokens.stream()
                .filter(e -> e.getStatus() == WriteConfirmationStatus.CONFIRMED)
                .toList();

        for (WriteConfirmationEntry entry : confirmedTokens) {
            if (Objects.equals(entry.getConnectionId(), db.connectionId())
                    && Objects.equals(entry.getCatalog(), db.catalog())
                    && Objects.equals(entry.getSchema(), db.schema())
                    && normalizeSql(entry.getSql()).equals(normalizedSql)) {
                entry.setStatus(WriteConfirmationStatus.CONSUMED);
                log.info("[WriteConfirm] Consumed by sql match: userId={} conversationId={}", userId, conversationId);
                return WriteConsumeResult.ok();
            }
        }

        // Step 4: no exact match — diagnose the mismatch using the first CONFIRMED token
        WriteConfirmationEntry closest = confirmedTokens.get(0);

        if (!Objects.equals(closest.getConnectionId(), db.connectionId())) {
            return WriteConsumeResult.fail("PARAM_MISMATCH",
                    "Confirmed token connectionId=" + closest.getConnectionId()
                            + " but executeNonSelectSql received connectionId=" + db.connectionId()
                            + ". Use the same connectionId.");
        }
        if (!Objects.equals(closest.getCatalog(), db.catalog())) {
            return WriteConsumeResult.fail("PARAM_MISMATCH",
                    "Confirmed token catalog='" + closest.getCatalog()
                            + "' but executeNonSelectSql received catalog='" + db.catalog()
                            + "'. Use the same catalog.");
        }
        if (!Objects.equals(closest.getSchema(), db.schema())) {
            return WriteConsumeResult.fail("PARAM_MISMATCH",
                    "Confirmed token schema='" + closest.getSchema()
                            + "' but executeNonSelectSql received schema='" + db.schema()
                            + "'. Use the same schema.");
        }
        // SQL must differ
        log.warn("[WriteConfirm] consumeConfirmedBySql: SQL mismatch for userId={} conversationId={}", userId, conversationId);
        return WriteConsumeResult.fail("SQL_MISMATCH",
                "SQL content differs from the confirmed SQL. Confirmed: '" + closest.getSql()
                        + "'; Received: '" + sql
                        + "'. You must call askUserConfirm again with the new SQL.");
    }

    /**
     * Normalize SQL for comparison: trim, collapse whitespace, strip trailing semicolons.
     * This prevents mismatches caused by the model reformatting SQL between tool calls.
     */
    private static String normalizeSql(String sql) {
        if (sql == null) return "";
        return sql.strip().replaceAll("\\s+", " ").replaceAll(";+$", "");
    }
}
