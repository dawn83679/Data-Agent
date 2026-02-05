package edu.zsc.ai.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.zsc.ai.model.TodoList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TodoCache - Caffeine-based cache for TodoList
 * Provides in-memory caching to reduce database queries
 * Automatically expires entries after configured time
 * 
 * @author AI Assistant
 */
@Component
@Slf4j
public class TodoCache {
    
    /**
     * Caffeine cache instance
     * Key: conversationId, Value: TodoList
     * Expires after 5 minutes of write
     * Maximum size: 1000 entries
     */
    private final Cache<Long, TodoList> cache;
    
    public TodoCache() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats()
            .build();
        
        log.info("TodoCache initialized with Caffeine: expireAfterWrite=5min, maxSize=1000");
    }
    
    /**
     * Get TodoList from cache
     * 
     * @param conversationId Conversation ID
     * @return TodoList if cached, null if not found
     */
    public TodoList get(Long conversationId) {
        if (conversationId == null) {
            log.warn("Attempting to get cache with null conversationId");
            return null;
        }
        
        TodoList todoList = cache.getIfPresent(conversationId);
        if (todoList != null) {
            log.debug("Cache hit for conversation: {}", conversationId);
        } else {
            log.debug("Cache miss for conversation: {}", conversationId);
        }
        return todoList;
    }
    
    /**
     * Put TodoList into cache
     * 
     * @param conversationId Conversation ID
     * @param todoList TodoList to cache
     */
    public void put(Long conversationId, TodoList todoList) {
        if (conversationId == null || todoList == null) {
            log.warn("Attempting to cache null value: conversationId={}, todoList={}", 
                conversationId, todoList);
            return;
        }
        cache.put(conversationId, todoList);
        log.debug("Cached TodoList for conversation: {}", conversationId);
    }
    
    /**
     * Remove TodoList from cache
     * 
     * @param conversationId Conversation ID
     */
    public void invalidate(Long conversationId) {
        if (conversationId == null) {
            return;
        }
        cache.invalidate(conversationId);
        log.debug("Invalidated cache for conversation: {}", conversationId);
    }
    
    /**
     * Check if TodoList is cached for given conversation
     * 
     * @param conversationId Conversation ID
     * @return true if cached, false otherwise
     */
    public boolean contains(Long conversationId) {
        if (conversationId == null) {
            return false;
        }
        return cache.getIfPresent(conversationId) != null;
    }
    
    /**
     * Clear all cache entries
     * Useful for testing or manual cleanup
     */
    public void invalidateAll() {
        long size = cache.estimatedSize();
        cache.invalidateAll();
        log.info("Invalidated all cache entries, removed approximately {} entries", size);
    }
    
    /**
     * Get current cache statistics
     * 
     * @return Cache stats as string
     */
    public String getStats() {
        return cache.stats().toString();
    }
    
    /**
     * Get current cache size
     * 
     * @return Estimated number of cached entries
     */
    public long size() {
        return cache.estimatedSize();
    }
}
