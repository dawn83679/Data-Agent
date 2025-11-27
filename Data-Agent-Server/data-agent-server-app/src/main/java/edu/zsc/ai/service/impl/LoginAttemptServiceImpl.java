package edu.zsc.ai.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.zsc.ai.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Login Attempt Service Implementation
 * Uses Caffeine cache to track login attempts
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

    // Cache for failed attempts: key -> attempt count
    private final Cache<String, Integer> attemptsCache;

    // Cache for block time: key -> block until time
    private final Cache<String, LocalDateTime> blockCache;

    public LoginAttemptServiceImpl() {
        // Initialize caches with expiration
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.blockCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    @Override
    public void loginSucceeded(String key) {
        // Clear failed attempts on successful login
        attemptsCache.invalidate(key);
        blockCache.invalidate(key);
        log.debug("Login succeeded for key: {}, cleared attempts", key);
    }

    @Override
    public void loginFailed(String key) {
        int attempts = attemptsCache.get(key, k -> 0) + 1;
        attemptsCache.put(key, attempts);

        log.warn("Login failed for key: {}, attempt: {}/{}", key, attempts, maxAttempts);

        if (attempts >= maxAttempts) {
            LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(blockDurationMinutes);
            blockCache.put(key, blockUntil);
            log.warn("Key {} is now blocked until {}", key, blockUntil);
        }
    }

    @Override
    public boolean isBlocked(String key) {
        LocalDateTime blockUntil = blockCache.getIfPresent(key);
        if (blockUntil == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(blockUntil)) {
            // Block expired, clean up
            blockCache.invalidate(key);
            attemptsCache.invalidate(key);
            return false;
        }

        return true;
    }

    @Override
    public int getRemainingAttempts(String key) {
        if (isBlocked(key)) {
            return 0;
        }

        int attempts = attemptsCache.get(key, k -> 0);
        return Math.max(0, maxAttempts - attempts);
    }

    @Override
    public long getBlockTimeRemaining(String key) {
        LocalDateTime blockUntil = blockCache.getIfPresent(key);
        if (blockUntil == null) {
            return 0;
        }

        Duration duration = Duration.between(LocalDateTime.now(), blockUntil);
        return Math.max(0, duration.getSeconds());
    }
}
