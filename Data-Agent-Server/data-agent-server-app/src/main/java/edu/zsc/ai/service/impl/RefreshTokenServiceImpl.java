package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.mapper.RefreshTokenMapper;
import edu.zsc.ai.model.entity.RefreshToken;
import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.util.HashUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh Token Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
// @Service // Disabled: Using CachedRefreshTokenServiceImpl instead
public class RefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken> implements RefreshTokenService {

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
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30 days
        refreshToken.setStatus(0); // Active

        // 3. Save to database
        this.save(refreshToken);
        log.info("Created refresh token for user: {}, sessionId: {}", userId, sessionId);

        return refreshTokenPlain;
    }

    @Override
    public RefreshToken verifyAndGet(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);

        RefreshToken refreshToken = this.getOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .eq(RefreshToken::getStatus, 0) // Only active tokens
                .gt(RefreshToken::getExpiresAt, LocalDateTime.now()) // Check expiration in query
                .last("LIMIT 1"));

        if (refreshToken == null) {
            throw new BusinessException(40100, "Invalid or expired refresh token");
        }

        return refreshToken;
    }

    @Override
    public void revoke(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);

        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .set(RefreshToken::getStatus, 1) // Used
                .set(RefreshToken::getUsedAt, LocalDateTime.now()));

        log.info("Revoked refresh token");
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getStatus, 0) // Only active tokens
                .set(RefreshToken::getStatus, 2)); // Revoked

        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    @Override
    public void cleanExpiredTokens() {
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .lt(RefreshToken::getExpiresAt, LocalDateTime.now())
                .eq(RefreshToken::getStatus, 0) // Only active tokens
                .set(RefreshToken::getStatus, 1)); // Mark as expired

        log.info("Cleaned expired refresh tokens");
    }
}
