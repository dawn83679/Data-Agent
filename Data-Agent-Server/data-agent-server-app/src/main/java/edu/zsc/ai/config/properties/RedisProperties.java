package edu.zsc.ai.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Redis Configuration Properties
 * Custom properties for Redis configuration
 *
 * @author Data-Agent Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    /**
     * Default cache TTL (Time To Live) in seconds
     * Default: 1800 seconds (30 minutes)
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * Session cache TTL in seconds
     * Default: 7200 seconds (2 hours)
     */
    private Duration sessionTtl = Duration.ofHours(2);

    /**
     * Verification code TTL in seconds
     * Default: 300 seconds (5 minutes)
     */
    private Duration verificationCodeTtl = Duration.ofMinutes(5);

    /**
     * Login attempt tracking TTL in seconds
     * Default: 900 seconds (15 minutes)
     */
    private Duration loginAttemptTtl = Duration.ofMinutes(15);

    /**
     * Key prefix for all Redis keys
     * Default: "data-agent:"
     */
    private String keyPrefix = "data-agent:";

    /**
     * Session key prefix in Redis
     * Default: "session:"
     */
    private String sessionPrefix = "session:";

    /**
     * Session cache expiration time in minutes
     * Default: 30 minutes (matches AccessToken expiration)
     */
    private long sessionCacheExpireMinutes = 30;

    /**
     * Refresh token key prefix in Redis
     * Default: "refresh_token:"
     */
    private String refreshTokenPrefix = "refresh_token:";

    /**
     * Refresh token cache expiration time in days
     * Default: 30 days
     */
    private long refreshTokenCacheExpireDays = 30;

    /**
     * Whether to enable Redis key expiration events
     * Default: false
     */
    private boolean enableKeyspaceNotifications = false;
}
