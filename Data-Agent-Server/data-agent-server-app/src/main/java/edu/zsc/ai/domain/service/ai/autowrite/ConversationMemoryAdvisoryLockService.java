package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.config.ai.MemoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uses PostgreSQL {@code pg_try_advisory_xact_lock(bigint)} so only one node processes a
 * conversation's memory extraction at a time. The lock is released when the transaction ends.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryAdvisoryLockService {

    private final JdbcTemplate jdbcTemplate;
    private final MemoryProperties memoryProperties;

    /**
     * @return false if the lock could not be acquired (another worker holds it)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean tryRunWithConversationLock(Long conversationId, Runnable runnable) {
        if (!memoryProperties.getAutowrite().isAdvisoryLockEnabled()) {
            runnable.run();
            return true;
        }
        Boolean locked = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?::bigint)",
                Boolean.class,
                conversationId);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("[MemAutoWrite] Advisory lock busy, skipping extraction: conversationId={}", conversationId);
            return false;
        }
        runnable.run();
        return true;
    }
}
