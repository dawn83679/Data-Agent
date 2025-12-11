package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;

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
 * Session Service Implementation (Direct Database Access)
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

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
        
        log.debug("Created session: userId={}, sessionId={}, ipAddress={}", userId, session.getId(), ipAddress);
        return session;
    }

    @Override
    public void updateActivity(Long sessionId) {
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getLastRefreshAt, LocalDateTime.now()));
    }

    @Override
    public void updateRefreshTime(Long sessionId, String newAccessTokenHash) {
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getAccessTokenHash, newAccessTokenHash)
                .set(Session::getLastRefreshAt, LocalDateTime.now()));
    }

    @Override
    public void revokeSession(Long sessionId) {
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.debug("Revoked session: sessionId={}", sessionId);
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        // Update database
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.debug("Revoked all sessions for user: {}", userId);
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
        
        // Query from database
        Session session = this.getOne(new LambdaQueryWrapper<Session>()
                .eq(Session::getAccessTokenHash, accessTokenHash)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .last("LIMIT 1"));
        
        if (session != null) {
            log.debug("Session loaded from database");
        }
        
        return session;
    }

    @Override
    public void cleanExpiredSessions() {
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
        // Query from database
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
        
        // Revoke the session
        revokeSession(sessionId);
        log.info("User {} revoked session: sessionId={}", userId, sessionId);
    }

    @Override
    public void revokeOtherSessions(Long userId, Long currentSessionId) {
        // Update database - revoke all active sessions except the current one
        this.update(new LambdaUpdateWrapper<Session>()
                .eq(Session::getUserId, userId)
                .eq(Session::getStatus, SessionStatus.ACTIVE.getCode())
                .ne(Session::getId, currentSessionId)
                .set(Session::getStatus, SessionStatus.REVOKED.getCode()));
        
        log.info("User {} revoked all other sessions, keeping sessionId={}", userId, currentSessionId);
    }
}
