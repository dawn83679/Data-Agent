package edu.zsc.ai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cache Monitor Service
 * Provides cache statistics and monitoring functionality
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMonitorService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Count Session cache entries
            Set<String> sessionKeys = redisTemplate.keys("session:*");
            stats.put("sessionCacheCount", sessionKeys != null ? sessionKeys.size() : 0);
            
            // Count RefreshToken cache entries
            Set<String> refreshTokenKeys = redisTemplate.keys("refresh_token:*");
            stats.put("refreshTokenCacheCount", refreshTokenKeys != null ? refreshTokenKeys.size() : 0);
            
            // Total cache count
            int totalCount = (sessionKeys != null ? sessionKeys.size() : 0) + 
                           (refreshTokenKeys != null ? refreshTokenKeys.size() : 0);
            stats.put("totalCacheCount", totalCount);
            
            log.debug("Cache stats: {}", stats);
        } catch (Exception e) {
            log.error("Error getting cache stats", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * Clear all Session cache
     */
    public void clearSessionCache() {
        try {
            Set<String> keys = redisTemplate.keys("session:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} session cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing session cache", e);
        }
    }

    /**
     * Clear all RefreshToken cache
     */
    public void clearRefreshTokenCache() {
        try {
            Set<String> keys = redisTemplate.keys("refresh_token:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} refresh token cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing refresh token cache", e);
        }
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        clearSessionCache();
        clearRefreshTokenCache();
        log.info("Cleared all cache");
    }
}
