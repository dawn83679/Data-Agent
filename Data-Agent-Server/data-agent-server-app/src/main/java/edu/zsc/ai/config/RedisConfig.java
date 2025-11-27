package edu.zsc.ai.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis Configuration
 * Configures RedisTemplate serialization and caching
 * Only active when Redis is available
 *
 * @author Data-Agent Team
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    /**
     * Configure RedisTemplate with Jackson serialization
     * Used for storing complex objects in Redis
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create and configure ObjectMapper
        ObjectMapper mapper = createObjectMapper();

        // Use Jackson2JsonRedisSerializer for value serialization
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // Use StringRedisSerializer for key serialization
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Configure serializers
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure StringRedisTemplate for simple string operations
     * More efficient for string-only operations
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    /**
     * Configure CacheManager for Spring Cache abstraction
     * Enables @Cacheable, @CacheEvict, etc. annotations
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper for cache serialization
        ObjectMapper mapper = createObjectMapper();
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // Configure Redis cache with default settings
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Set default TTL to 30 minutes
                .entryTtl(Duration.ofMinutes(30))
                // Don't cache null values
                .disableCachingNullValues()
                // Use custom key prefix
                .computePrefixWith(cacheName -> "data-agent:cache:" + cacheName + ":")
                // Configure key serialization
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Configure value serialization
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                // Enable transactions
                .transactionAware()
                .build();
    }

    /**
     * Create and configure ObjectMapper for Redis serialization
     * Supports Java 8 time types and proper type handling
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Set visibility for all fields
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // Enable default typing for polymorphic types
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        
        // Register Java 8 time module for LocalDateTime, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());
        
        return mapper;
    }
}
