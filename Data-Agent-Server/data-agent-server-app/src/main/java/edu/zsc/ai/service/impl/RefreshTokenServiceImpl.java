package edu.zsc.ai.service.impl;

import java.time.LocalDateTime;

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
 * Refresh Token Service Implementation (Direct Database Access)
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
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
        refreshToken.setRevoked(RefreshTokenStatus.ACTIVE.getCode());

        // 3. Save to database
        this.save(refreshToken);
        
        log.info("Created refresh token for user: {}, sessionId: {}", userId, sessionId);

        return refreshTokenPlain;
    }

    @Override
    public RefreshToken verifyAndGet(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);

        // Query from database
        RefreshToken refreshToken = this.getOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .gt(RefreshToken::getExpiresAt, LocalDateTime.now())
                .last("LIMIT 1"));

        if (refreshToken == null) {
            throw new BusinessException(40100, "Invalid or expired refresh token");
        }

        log.debug("RefreshToken loaded from database");

        return refreshToken;
    }

    @Override
    public void revoke(String refreshTokenPlain) {
        String tokenHash = HashUtil.sha256(refreshTokenPlain);

        // Update database
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode())
                .set(RefreshToken::getLastUsedAt, LocalDateTime.now()));
        
        log.info("Revoked refresh token");
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        // Update database
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode()));

        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    @Override
    public void cleanExpiredTokens() {
        this.update(new LambdaUpdateWrapper<RefreshToken>()
                .lt(RefreshToken::getExpiresAt, LocalDateTime.now())
                .eq(RefreshToken::getRevoked, RefreshTokenStatus.ACTIVE.getCode())
                .set(RefreshToken::getRevoked, RefreshTokenStatus.REVOKED.getCode()));

        log.info("Cleaned expired refresh tokens from database");
    }
}
