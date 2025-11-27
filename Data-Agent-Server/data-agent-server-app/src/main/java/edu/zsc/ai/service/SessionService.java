package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.model.entity.Session;

/**
 * Session Service
 *
 * @author Data-Agent Team
 */
public interface SessionService extends IService<Session> {

    /**
     * Create new session
     */
    Session createSession(Long userId, String accessTokenHash, String ipAddress, String userAgent);

    /**
     * Update session activity
     */
    void updateActivity(Long sessionId);

    /**
     * Update session refresh time
     */
    void updateRefreshTime(Long sessionId, String newAccessTokenHash);

    /**
     * Revoke session
     */
    void revokeSession(Long sessionId);

    /**
     * Revoke all user sessions
     */
    void revokeAllUserSessions(Long userId);

    /**
     * Get session by access token hash
     */
    Session getByAccessTokenHash(String accessTokenHash);

    /**
     * Get session by access token (for Sa-Token integration)
     */
    Session getByAccessToken(String accessToken);

    /**
     * Clean expired sessions
     */
    void cleanExpiredSessions();
}
