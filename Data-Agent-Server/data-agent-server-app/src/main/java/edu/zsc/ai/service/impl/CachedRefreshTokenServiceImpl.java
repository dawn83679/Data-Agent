package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.enums.RefreshTokenStatus;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.mapper.RefreshTokenMapper;
import edu.zsc.ai.model.entity.RefreshToken;
import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Refresh Token Service Implementation with Redis
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedRefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken> implements RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final edu.zsc.ai.config.properties.RedisProperties redisProperties;

    @Override
    public String createRefreshToken(Long userId, Long sessionId) {
        // 1. Generate random token
        String refreshTokenPlain = HashUtil.generateRandomToken();
        String tokenHash = HashUtil.sha256(refreshTokenPlain);

        // 2. Create refresh token entity
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setSessionId(sessionId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(redisProperties.getRefreshTokenCacheExpireDays()));
        refreshToken.setRevoked(RefreshTokenStatus.ACTIVE.getCode());

        // 3. Save to database
        this.save(refreshToken);
        
        // 4. Cache in Redis for fast lookup
        String cacheKey = redisProperties.getRefreshTokenPrefix() + tokenHash;
        redisTemplate.opsForValue().set(cacheKey, refreshToken, redisProperties.getRefreshTokenCacheExpireDays(), TimeUnit.DAYS);
        
        log.info("Created and cached refresh token for user: {}, sessionId: {}", userId, sessionId);

        return refreshTokenPlain;
    }

    @Override
    public RefreshToken verifyAndGet(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);
        String cacheKey = redisProperties.getRefreshTokenPrefix() + tokenHash;

        // 1. Try to get from Redis cache first
        RefreshToken cachedToken = (RefreshToken) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedToken != null) {
            // Verify expiration and revoked status
            if (RefreshTokenStatus.isActive(cachedToken.getRevoked()) && cachedToken.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.debug("RefreshToken found in cache");
                return cachedToken;
            } else {
                // Remove invalid token from cache
                redisTemplate.delete(cacheKey);
            }
        }

        // 2. If not in cache, query from database
        RefreshToken refreshToken = this.getOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .gt(RefreshToken::getExpiresAt, LocalDateTime.now())
                .last("LIMIT 1"));

        if (refreshToken == null) {
            throw new BusinessException(40100, "Invalid or expired refresh token");
        }

        // 3. Update cache
        redisTemplate.opsForValue().set(cacheKey, refreshToken, redisProperties.getRefreshTokenCacheExpireDays(), TimeUnit.DAYS);
        log.debug("RefreshToken loaded from database and cached");

        return refreshToken;
    }

    @Override
    public void revoke(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);
        String cacheKey = redisProperties.getRefreshTokenPrefix() + tokenHash;

        // 1. Update database
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode())
                .set(RefreshToken::getLastUsedAt, LocalDateTime.now()));

        // 2. Remove from cache
        redisTemplate.delete(cacheKey);
        
        log.info("Revoked and removed refresh token from cache");
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        // 1. Get all user tokens from database
        var tokens = this.list(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode()));

        // 2. Update database
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode()));

        // 3. Remove all from cache
        tokens.forEach(token -> {
            String cacheKey = redisProperties.getRefreshTokenPrefix() + token.getTokenHash();
            redisTemplate.delete(cacheKey);
        });

        log.info("Revoked all refresh tokens for user: {} and cleared cache", userId);
    }

    @Override
    public void cleanExpiredTokens() {
        // Only update database, Redis will auto-expire
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .lt(RefreshToken::getExpiresAt, LocalDateTime.now())
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode()));

        log.info("Cleaned expired refresh tokens from database");
    }
}
