package edu.zsc.ai.plugin.value;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Redis-specific implementation of ValueContext.
 * Encapsulates all parameters needed for Redis value processing.
 *
 * <p>This class is specific to Redis data sources and provides
 * Redis-specific metadata like key, value type, TTL, etc.
 *
 * @author hhz
 */
@Data
@Builder
public class RedisValueContext implements ValueContext {

    /**
     * The Redis key
     */
    private String key;

    /**
     * The field name (for Hash type)
     */
    private String fieldName;

    /**
     * The field index (for List/Set type)
     */
    private int fieldIndex;

    /**
     * The Redis value type (STRING, HASH, LIST, SET, ZSET, etc.)
     */
    private String redisType;

    /**
     * The raw value object
     */
    private Object value;

    /**
     * TTL (Time To Live) in seconds, -1 for no expiration
     */
    private Long ttl;

    /**
     * Additional metadata storage
     */
    @Builder.Default
    private Map<String, Object> additionalMetadata = new HashMap<>();

    // ==================== ValueContext Interface Implementation ====================

    @Override
    public String getDataSourceType() {
        return "REDIS";
    }

    @Override
    public int getIndex() {
        return fieldIndex;
    }

    @Override
    public String getName() {
        return fieldName != null ? fieldName : key;
    }

    @Override
    public String getTypeName() {
        return redisType;
    }

    @Override
    public Object getMetadata(String key) {
        return additionalMetadata.get(key);
    }

    @Override
    public boolean isNullable() {
        return true; // Redis values can be null
    }

    /**
     * Add additional metadata.
     *
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, Object value) {
        additionalMetadata.put(key, value);
    }
}
