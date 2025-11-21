package edu.zsc.ai.util;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis Utility Class
 * Provides convenient methods for Redis operations
 *
 * @author Data-Agent Team
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ========== Common Operations ==========

    /**
     * Set expiration time for a key
     *
     * @param key  Key
     * @param time Expiration time (seconds)
     * @return true if successful
     */
    public boolean expire(String key, long time) {
        if (time > 0) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
        }
        return false;
    }

    /**
     * Set expiration time for a key with Duration
     *
     * @param key      Key
     * @param duration Duration
     * @return true if successful
     */
    public boolean expire(String key, Duration duration) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, duration));
    }

    /**
     * Get expiration time of a key
     *
     * @param key Key
     * @return Expiration time in seconds, -1 if key doesn't exist, -2 if key has no expiration
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2;
    }

    /**
     * Check if key exists
     *
     * @param key Key
     * @return true if exists
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete one or more keys
     *
     * @param keys Keys to delete
     */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                redisTemplate.delete(keys[0]);
            } else {
                redisTemplate.delete(List.of(keys));
            }
        }
    }

    /**
     * Delete keys by collection
     *
     * @param keys Collection of keys
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    // ========== String Operations ==========

    /**
     * Get value by key
     *
     * @param key Key
     * @return Value
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * Set key-value
     *
     * @param key   Key
     * @param value Value
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Set key-value with expiration time
     *
     * @param key   Key
     * @param value Value
     * @param time  Expiration time in seconds
     */
    public void set(String key, Object value, long time) {
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
    }

    /**
     * Set key-value with Duration
     *
     * @param key      Key
     * @param value    Value
     * @param duration Duration
     */
    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * Increment value by delta
     *
     * @param key   Key
     * @param delta Delta value
     * @return New value after increment
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * Decrement value by delta
     *
     * @param key   Key
     * @param delta Delta value
     * @return New value after decrement
     */
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().decrement(key, delta);
        return result != null ? result : 0;
    }

    // ========== Hash Operations ==========

    /**
     * Get hash field value
     *
     * @param key  Key
     * @param item Hash field
     * @return Field value
     */
    public Object hGet(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * Get all hash fields and values
     *
     * @param key Key
     * @return Map of fields and values
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Set hash field value
     *
     * @param key   Key
     * @param item  Hash field
     * @param value Field value
     */
    public void hSet(String key, String item, Object value) {
        redisTemplate.opsForHash().put(key, item, value);
    }

    /**
     * Set multiple hash fields
     *
     * @param key Key
     * @param map Map of fields and values
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * Delete hash fields
     *
     * @param key   Key
     * @param items Hash fields to delete
     */
    public void hDelete(String key, Object... items) {
        redisTemplate.opsForHash().delete(key, items);
    }

    /**
     * Check if hash field exists
     *
     * @param key  Key
     * @param item Hash field
     * @return true if exists
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    // ========== Set Operations ==========

    /**
     * Get all members of a set
     *
     * @param key Key
     * @return Set members
     */
    public Set<Object> sGet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Check if value is member of set
     *
     * @param key   Key
     * @param value Value to check
     * @return true if member
     */
    public boolean sHasKey(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * Add values to set
     *
     * @param key    Key
     * @param values Values to add
     * @return Number of values added
     */
    public long sSet(String key, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        return count != null ? count : 0;
    }

    /**
     * Remove values from set
     *
     * @param key    Key
     * @param values Values to remove
     * @return Number of values removed
     */
    public long sRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key, values);
        return count != null ? count : 0;
    }

    // ========== List Operations ==========

    /**
     * Get list range
     *
     * @param key   Key
     * @param start Start index
     * @param end   End index
     * @return List elements
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * Get list size
     *
     * @param key Key
     * @return List size
     */
    public long lSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    /**
     * Get list element by index
     *
     * @param key   Key
     * @param index Index
     * @return Element value
     */
    public Object lGet(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * Add value to list (right push)
     *
     * @param key   Key
     * @param value Value
     */
    public void lPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * Add values to list (right push)
     *
     * @param key    Key
     * @param values Values
     */
    public void lPushAll(String key, Object... values) {
        redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * Remove value from list
     *
     * @param key   Key
     * @param count Number of occurrences to remove
     * @param value Value to remove
     * @return Number of values removed
     */
    public long lRemove(String key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        return removed != null ? removed : 0;
    }
}
