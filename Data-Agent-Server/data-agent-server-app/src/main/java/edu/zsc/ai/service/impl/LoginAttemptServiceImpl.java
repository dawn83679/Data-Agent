package edu.zsc.ai.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import edu.zsc.ai.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Login Attempt Service Implementation
 * Uses Redis to track login attempts for distributed deployment support
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.block-duration-minutes:5}")
    private int blockDurationMinutes;

    private static final String ATTEMPT_PREFIX = "login:attempt:";
    private static final String BLOCK_PREFIX = "login:block:";
    private static final int ATTEMPT_TTL_MINUTES = 15;

    @Override
    public void loginSucceeded(String key) {
        // Clear failed attempts on successful login
        String attemptKey = ATTEMPT_PREFIX + key;
        String blockKey = BLOCK_PREFIX + key;
        
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(blockKey);
        
        log.debug("Login succeeded for key: {}, cleared attempts", key);
    }

    @Override
    public void loginFailed(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        
        // Increment attempt count
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        // Set expiration on first attempt
        if (attempts == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_TTL_MINUTES, TimeUnit.MINUTES);
        }

        log.warn("Login failed for key: {}, attempt: {}/{}", key, attempts, maxAttempts);

        // Block if max attempts reached
        if (attempts >= maxAttempts) {
            String blockKey = BLOCK_PREFIX + key;
            LocalDateTime blockUntil = LocalDateTime.now().plusMinutes(blockDurationMinutes);
            
            redisTemplate.opsForValue().set(blockKey, blockUntil);
            redisTemplate.expire(blockKey, blockDurationMinutes, TimeUnit.MINUTES);
            
            log.warn("Key {} is now blocked until {}", key, blockUntil);
        }
    }

    @Override
    public boolean isBlocked(String key) {
        String blockKey = BLOCK_PREFIX + key;
        Object blockUntilObj = redisTemplate.opsForValue().get(blockKey);
        
        if (blockUntilObj == null) {
            return false;
        }

        LocalDateTime blockUntil = (LocalDateTime) blockUntilObj;
        
        if (LocalDateTime.now().isAfter(blockUntil)) {
            // Block expired, clean up
            redisTemplate.delete(blockKey);
            redisTemplate.delete(ATTEMPT_PREFIX + key);
            return false;
        }

        return true;
    }

    @Override
    public int getRemainingAttempts(String key) {
        if (isBlocked(key)) {
            return 0;
        }

        String attemptKey = ATTEMPT_PREFIX + key;
        Object attemptsObj = redisTemplate.opsForValue().get(attemptKey);
        
        int attempts = attemptsObj != null ? ((Number) attemptsObj).intValue() : 0;
        return Math.max(0, maxAttempts - attempts);
    }

    @Override
    public long getBlockTimeRemaining(String key) {
        String blockKey = BLOCK_PREFIX + key;
        Object blockUntilObj = redisTemplate.opsForValue().get(blockKey);
        
        if (blockUntilObj == null) {
            return 0;
        }

        LocalDateTime blockUntil = (LocalDateTime) blockUntilObj;
        Duration duration = Duration.between(LocalDateTime.now(), blockUntil);
        return Math.max(0, duration.getSeconds());
    }
}
