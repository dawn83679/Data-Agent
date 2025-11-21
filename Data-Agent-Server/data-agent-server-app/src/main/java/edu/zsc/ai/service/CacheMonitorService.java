package edu.zsc.ai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存监控服务
 * 提供缓存统计和监控功能
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMonitorService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 统计 Session 缓存数量
            Set<String> sessionKeys = redisTemplate.keys("session:*");
            stats.put("sessionCacheCount", sessionKeys != null ? sessionKeys.size() : 0);
            
            // 统计 RefreshToken 缓存数量
            Set<String> refreshTokenKeys = redisTemplate.keys("refresh_token:*");
            stats.put("refreshTokenCacheCount", refreshTokenKeys != null ? refreshTokenKeys.size() : 0);
            
            // 总缓存数量
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
     * 清除所有 Session 缓存
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
     * 清除所有 RefreshToken 缓存
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
     * 清除所有缓存
     */
    public void clearAllCache() {
        clearSessionCache();
        clearRefreshTokenCache();
        log.info("Cleared all cache");
    }
}
