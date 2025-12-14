package edu.zsc.ai.domain.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.sys.SysRefreshTokens;

/**
 * Service interface for sys_refresh_tokens operations
 *
 * @author zgq
 */
public interface SysRefreshTokensService extends IService<SysRefreshTokens> {

    /**
     * Create and store refresh token
     */
    String createAndStoreRefreshToken(edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest request);

    /**
     * Verify and get refresh token
     */
    SysRefreshTokens verifyAndGet(String refreshTokenPlain);

    /**
     * Revoke refresh token
     */
    void revoke(String refreshTokenPlain);

    /**
     * Revoke all refresh tokens by session ID
     */
    void revokeAllBySessionId(edu.zsc.ai.domain.model.dto.request.sys.RevokeRefreshTokenBySessionRequest request);
}