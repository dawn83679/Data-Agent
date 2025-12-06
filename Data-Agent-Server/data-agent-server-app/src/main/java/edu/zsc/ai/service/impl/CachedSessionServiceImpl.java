package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.enums.SessionStatus;
import edu.zsc.ai.mapper.SessionMapper;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Session Service Implementation with Redis
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedSessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final edu.zsc.ai.config.properties.RedisProperties redisProperties;

    @Override
    public Session createSession(Long userId, String accessTokenHash, String ipAddress, String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        Session session = new Session();
        session.setUserId(userId);
        session.setAccessTokenHash(accessTokenHash);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLastRefreshAt(now);
        session.setExpiresAt(now.plusDays(30)); // 30 days
        session.setStatus(SessionStatus.ACTIVE.getCode());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        this.save(session);
        
        // Cache in Redis
        String cacheKey = redisProperties.getSessionPrefix() + accessTokenHash;
        redisTemplate.opsForValue().set(cacheKey, session, redisProperties.getSessionCacheExpireMinutes(), TimeUnit.MINUTES);
        
        log.debug("Created and cached session: userId={}, sessionId={}, ipAddress={}", userId, session.getId(), ipAddress);
        return session;
    }

    @Override
    public void updateActivity(Long sessionId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getLastRefreshAt, LocalDateTime.now()));
        
        // Note: We don't update cache here to avoid frequent writes
        // Cache will be refreshed on next getByAccessToken call
    }

    @Override
    public void updateRefreshTime(Long sessionId, String newAccessTokenHash) {
        // Get old session to remove old cache
        Session oldSession = this.getById(sessionId);
        if (oldSession != null && oldSession.getAccessTokenHash() != null) {
            String oldCacheKey = redisProperties.getSessionPrefix() + oldSession.getAccessTokenHash();
            redisTemplate.delete(oldCacheKey);
        }
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getAccessTokenHash, newAccessTokenHash)
                .set(Session::getLastRefreshAt, LocalDateTime.now()));
        
        // Get updated session and cache it
        Session updatedSession = this.getById(sessionId);
        if (updatedSession != null) {
            String newCacheKey = redisProperties.getSessionPrefix() + newAccessTokenHash;
            redisTemplate.opsForValue().set(newCacheKey, updatedSession, redisProperties.getSessionCacheExpireMinutes(), TimeUnit.MINUTES);
        }
    }

    @Override
    public void revokeSession(Long sessionId) {
        // Get session to remove cache
        Session session = this.getById(sessionId);
        if (session != null && session.getAccessTokenHash() != null) {
            String cacheKey = redisProperties.getSessionPrefix() + session.getAccessTokenHash();
            redisTemplate.delete(cacheKey);
        }
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.debug("Revoked session and removed from cache: sessionId={}", sessionId);
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        // Get all user sessions to remove from cache
        var sessions = this.list(new LambdaQueryWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode()));
        
        // Remove all from cache
        sessions.forEach(session -> {
            if (session.getAccessTokenHash() != null) {
                String cacheKey = redisProperties.getSessionPrefix() + session.getAccessTokenHash();
                redisTemplate.delete(cacheKey);
            }
        });
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.debug("Revoked all sessions for user: {} and cleared cache", userId);
    }

    @Override
    public Session getByAccessTokenHash(String accessTokenHash) {
        return getByAccessToken(accessTokenHash);
    }

    @Override
    public Session getByAccessToken(String accessToken) {
        // Note: accessToken parameter is now expected to be the hash
        // The caller (AuthServiceImpl) should hash the token before calling this method
        String accessTokenHash = accessToken;
        String cacheKey = redisProperties.getSessionPrefix() + accessTokenHash;
        
        // 1. Try to get from Redis cache first
        Session cachedSession = (Session) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSession != null) {
            // Verify status
            if (SessionStatus.isActive(cachedSession.getStatus())) {
                log.debug("Session found in cache");
                return cachedSession;
            } else {
                // Remove invalid session from cache
                redisTemplate.delete(cacheKey);
            }
        }
        
        // 2. If not in cache, query from database
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getAccessTokenHash, accessTokenHash)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .last("LIMIT 1"));
        
        if (session != null) {
            // 3. Update cache
            redisTemplate.opsForValue().set(cacheKey, session, redisProperties.getSessionCacheExpireMinutes(), TimeUnit.MINUTES);
            log.debug("Session loaded from database and cached");
        }
        
        return session;
    }

    @Override
    public void cleanExpiredSessions() {
        // Only update database, Redis will auto-expire
        long count = this.count(new LambdaQueryWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode()));
        
        this.update(new LambdaUpdateWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .set(Session::getStatus, SessionStatus.EXPIRED.getCode()));
        
        if (count > 0) {
            log.info("Cleaned {} expired sessions from database", count);
        }
    }

    @Override
    public java.util.List<Session> getUserActiveSessions(Long userId) {
        // Query from database (no caching for list operations)
        return this.list(new LambdaQueryWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .orderByDesc(Session::getLastRefreshAt));
    }

    @Override
    public void revokeSessionWithPermissionCheck(Long userId, Long sessionId) {
        // First check if session exists and belongs to the user
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getId, sessionId)
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .last("LIMIT 1"));
        
        if (session == null) {
            throw new edu.zsc.ai.exception.BusinessException(
                edu.zsc.ai.enums.error.ErrorCode.NOT_FOUND_ERROR, 
                "Session not found or already revoked");
        }
        
        // Revoke the session (this will also clear cache)
        revokeSession(sessionId);
        log.info("User {} revoked session: sessionId={}", userId, sessionId);
    }

    @Override
    public void revokeOtherSessions(Long userId, Long currentSessionId) {
        // Get all user sessions except current one to remove from cache
        var sessions = this.list(new LambdaQueryWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .ne(Session::getId, currentSessionId));
        
        // Remove all from cache
        sessions.forEach(session -> {
            if (session.getAccessTokenHash() != null) {
                String cacheKey = redisProperties.getSessionPrefix() + session.getAccessTokenHash();
                redisTemplate.delete(cacheKey);
            }
        });
        
        // Update database - revoke all active sessions except the current one
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .ne(Session::getId, currentSessionId)
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.info("User {} revoked all other sessions, keeping sessionId={}", userId, currentSessionId);
    }
}
