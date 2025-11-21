package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

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
    
    private static final String SESSION_PREFIX = "session:";
    private static final long CACHE_EXPIRE_HOURS = 2; // Match AccessToken expiration

    @Override
    public Session createSession(Long userId, String accessTokenHash, String ipAddress, String userAgent) {
        Session session = new Session();
        session.setUserId(userId);
        session.setAccessTokenHash(accessTokenHash);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLastActivityAt(LocalDateTime.now());
        session.setLastRefreshAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days
        session.setStatus(0); // Active

        this.save(session);
        
        // Cache in Redis
        String cacheKey = SESSION_PREFIX + accessTokenHash;
        redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        log.debug("Created and cached session: userId={}, sessionId={}, ipAddress={}", userId, session.getId(), ipAddress);
        return session;
    }

    @Override
    public void updateActivity(Long sessionId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getLastActivityAt, LocalDateTime.now()));
        
        // Note: We don't update cache here to avoid frequent writes
        // Cache will be refreshed on next getByAccessToken call
    }

    @Override
    public void updateRefreshTime(Long sessionId, String newAccessTokenHash) {
        // Get old session to remove old cache
        Session oldSession = this.getById(sessionId);
        if (oldSession != null && oldSession.getAccessTokenHash() != null) {
            String oldCacheKey = SESSION_PREFIX + oldSession.getAccessTokenHash();
            redisTemplate.delete(oldCacheKey);
        }
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getAccessTokenHash, newAccessTokenHash)
                .set(Session::getLastRefreshAt, LocalDateTime.now())
                .set(Session::getLastActivityAt, LocalDateTime.now()));
        
        // Get updated session and cache it
        Session updatedSession = this.getById(sessionId);
        if (updatedSession != null) {
            String newCacheKey = SESSION_PREFIX + newAccessTokenHash;
            redisTemplate.opsForValue().set(newCacheKey, updatedSession, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    @Override
    public void revokeSession(Long sessionId) {
        // Get session to remove cache
        Session session = this.getById(sessionId);
        if (session != null && session.getAccessTokenHash() != null) {
            String cacheKey = SESSION_PREFIX + session.getAccessTokenHash();
            redisTemplate.delete(cacheKey);
        }
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getStatus, 2)); // Revoked
        
        log.debug("Revoked session and removed from cache: sessionId={}", sessionId);
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        // Get all user sessions to remove from cache
        var sessions = this.list(new LambdaQueryWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, 0));
        
        // Remove all from cache
        sessions.forEach(session -> {
            if (session.getAccessTokenHash() != null) {
                String cacheKey = SESSION_PREFIX + session.getAccessTokenHash();
                redisTemplate.delete(cacheKey);
            }
        });
        
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, 0) // Only active sessions
                .set(Session::getStatus, 2)); // Revoked
        
        log.debug("Revoked all sessions for user: {} and cleared cache", userId);
    }

    @Override
    public Session getByAccessTokenHash(String accessTokenHash) {
        return getByAccessToken(accessTokenHash);
    }

    @Override
    public Session getByAccessToken(String accessToken) {
        String cacheKey = SESSION_PREFIX + accessToken;
        
        // 1. Try to get from Redis cache first
        Session cachedSession = (Session) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedSession != null) {
            // Verify status
            if (cachedSession.getStatus() == 0) {
                log.debug("Session found in cache");
                return cachedSession;
            } else {
                // Remove invalid session from cache
                redisTemplate.delete(cacheKey);
            }
        }
        
        // 2. If not in cache, query from database
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getAccessTokenHash, accessToken)
                .eq(Session::getStatus, 0) // Only active sessions
                .last("LIMIT 1"));
        
        if (session != null) {
            // 3. Update cache
            redisTemplate.opsForValue().set(cacheKey, session, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("Session loaded from database and cached");
        }
        
        return session;
    }

    @Override
    public void cleanExpiredSessions() {
        // Only update database, Redis will auto-expire
        long count = this.count(new LambdaQueryWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, 0));
        
        this.update(new LambdaUpdateWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, 0) // Only active sessions
                .set(Session::getStatus, 1)); // Expired
        
        if (count > 0) {
            log.info("Cleaned {} expired sessions from database", count);
        }
    }
}
