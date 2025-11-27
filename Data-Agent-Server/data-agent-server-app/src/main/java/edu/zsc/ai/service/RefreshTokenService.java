package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.entity.RefreshToken;

/**
 * Refresh Token Service
 *
 * @author Data-Agent Team
 */
public interface RefreshTokenService extends IService<RefreshToken> {

    /**
     * Create and store refresh token
     */
    String createRefreshToken(Long userId, Long sessionId);

    /**
     * Verify and get refresh token
     */
    RefreshToken verifyAndGet(String refreshTokenPlain);

    /**
     * Revoke refresh token (mark as used)
     */
    void revoke(String refreshTokenPlain);

    /**
     * Revoke all user refresh tokens
     */
    void revokeAllUserTokens(Long userId);

    /**
     * Clean expired tokens
     */
    void cleanExpiredTokens();
}
