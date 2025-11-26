package edu.zsc.ai.service;

/**
 * Login Attempt Service
 * Track and manage login attempts to prevent brute force attacks
 *
 * @author Data-Agent Team
 */
public interface LoginAttemptService {

    /**
     * Record a successful login attempt
     *
     * @param key identifier (email or IP)
     */
    void loginSucceeded(String key);

    /**
     * Record a failed login attempt
     *
     * @param key identifier (email or IP)
     */
    void loginFailed(String key);

    /**
     * Check if the key is blocked
     *
     * @param key identifier (email or IP)
     * @return true if blocked
     */
    boolean isBlocked(String key);

    /**
     * Get remaining attempts before block
     *
     * @param key identifier (email or IP)
     * @return remaining attempts
     */
    int getRemainingAttempts(String key);

    /**
     * Get block time remaining in seconds
     *
     * @param key identifier (email or IP)
     * @return seconds remaining, 0 if not blocked
     */
    long getBlockTimeRemaining(String key);

    /**
     * Clear failure count for a key (useful for testing)
     *
     * @param key identifier (email or IP)
     */
    void clearFailureCount(String key);
}
