package edu.zsc.ai.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.zsc.ai.service.LoginAttemptService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Login Attempt Service Implementation
 * Uses in-memory storage to track login attempts
 * Note: This is a temporary solution. For production with multiple instances,
 * consider using database-backed implementation.
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.block-duration-minutes:5}")
    private int blockDurationMinutes;

    private static final int ATTEMPT_TTL_MINUTES = 15;
    
    private final Map<String, AttemptRecord> attemptStore = new ConcurrentHashMap<>();
    private final Map<String, BlockRecord> blockStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    @Data
    private static class AttemptRecord {
        private int count;
        private LocalDateTime expiresAt;
    }
    
    @Data
    private static class BlockRecord {
        private LocalDateTime blockedUntil;
    }
    
    @PostConstruct
    public void init() {
        // Schedule cleanup task every 5 minutes
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredRecords, 5, 5, TimeUnit.MINUTES);
        log.info("LoginAttemptService initialized with in-memory storage");
    }
    
    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdown();
    }
    
    private void cleanupExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean expired attempts
        attemptStore.entrySet().removeIf(entry -> 
            entry.getValue().getExpiresAt().isBefore(now));
        
        // Clean expired blocks
        blockStore.entrySet().removeIf(entry -> 
            entry.getValue().getBlockedUntil().isBefore(now));
        
        log.debug("Cleaned up expired login attempt records");
    }

    @Override
    public void loginSucceeded(String key) {
        // Clear failed attempts on successful login
        attemptStore.remove(key);
        blockStore.remove(key);
        
        log.debug("Login succeeded for key: {}, cleared attempts", key);
    }

    @Override
    public void loginFailed(String key) {
        LocalDateTime now = LocalDateTime.now();
        
        // Get or create attempt record
        AttemptRecord record = attemptStore.computeIfAbsent(key, k -> {
            AttemptRecord newRecord = new AttemptRecord();
            newRecord.setCount(0);
            newRecord.setExpiresAt(now.plusMinutes(ATTEMPT_TTL_MINUTES));
            return newRecord;
        });
        
        // Increment attempt count
        record.setCount(record.getCount() + 1);
        int attempts = record.getCount();
        
        log.warn("Login failed for key: {}, attempt: {}/{}", key, attempts, maxAttempts);

        // Block if max attempts reached
        if (attempts >= maxAttempts) {
            LocalDateTime blockUntil = now.plusMinutes(blockDurationMinutes);
            BlockRecord blockRecord = new BlockRecord();
            blockRecord.setBlockedUntil(blockUntil);
            blockStore.put(key, blockRecord);
            
            log.warn("Key {} is now blocked until {}", key, blockUntil);
        }
    }

    @Override
    public boolean isBlocked(String key) {
        BlockRecord blockRecord = blockStore.get(key);
        
        if (blockRecord == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockUntil = blockRecord.getBlockedUntil();
        
        if (now.isAfter(blockUntil)) {
            // Block expired, clean up
            blockStore.remove(key);
            attemptStore.remove(key);
            return false;
        }

        return true;
    }

    @Override
    public int getRemainingAttempts(String key) {
        if (isBlocked(key)) {
            return 0;
        }

        AttemptRecord record = attemptStore.get(key);
        int attempts = record != null ? record.getCount() : 0;
        return Math.max(0, maxAttempts - attempts);
    }

    @Override
    public long getBlockTimeRemaining(String key) {
        BlockRecord blockRecord = blockStore.get(key);
        
        if (blockRecord == null) {
            return 0;
        }

        LocalDateTime blockUntil = blockRecord.getBlockedUntil();
        Duration duration = Duration.between(LocalDateTime.now(), blockUntil);
        return Math.max(0, duration.getSeconds());
    }

    @Override
    public void clearFailureCount(String key) {
        attemptStore.remove(key);
        blockStore.remove(key);
        
        log.debug("Cleared failure count for key: {}", key);
    }
}
