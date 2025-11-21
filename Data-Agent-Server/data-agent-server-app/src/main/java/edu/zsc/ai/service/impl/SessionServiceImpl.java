package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.mapper.SessionMapper;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.service.SessionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Session Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
// @Service // Disabled: Using CachedSessionServiceImpl instead
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

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
        log.debug("Created new session: userId={}, sessionId={}, ipAddress={}", userId, session.getId(), ipAddress);
        return session;
    }

    @Override
    public void updateActivity(Long sessionId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getLastActivityAt, LocalDateTime.now()));
    }

    @Override
    public void updateRefreshTime(Long sessionId, String newAccessTokenHash) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getAccessTokenHash, newAccessTokenHash)
                .set(Session::getLastRefreshAt, LocalDateTime.now())
                .set(Session::getLastActivityAt, LocalDateTime.now()));
    }

    @Override
    public void revokeSession(Long sessionId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getStatus, 2)); // Revoked
        log.debug("Revoked session: sessionId={}", sessionId);
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, 0) // Only active sessions
                .set(Session::getStatus, 2)); // Revoked
        log.debug("Revoked all sessions for user: userId={}", userId);
    }

    @Override
    public Session getByAccessTokenHash(String accessTokenHash) {
        return this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getAccessTokenHash, accessTokenHash)
                .eq(Session::getStatus, 0) // Only active sessions
                .last("LIMIT 1"));
    }

    @Override
    public Session getByAccessToken(String accessToken) {
        // For Sa-Token integration, we store the token directly in accessTokenHash field
        return this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getAccessTokenHash, accessToken)
                .eq(Session::getStatus, 0) // Only active sessions
                .last("LIMIT 1"));
    }

    @Override
    public void cleanExpiredSessions() {
        long count = this.count(new LambdaQueryWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, 0));
        
        this.update(new LambdaUpdateWrapper<Session>()
                .lt(Session::getExpiresAt, LocalDateTime.now())
                .eq(Session::getStatus, 0) // Only active sessions
                .set(Session::getStatus, 1)); // Expired
        
        if (count > 0) {
            log.info("Cleaned {} expired sessions", count);
        }
    }
}
