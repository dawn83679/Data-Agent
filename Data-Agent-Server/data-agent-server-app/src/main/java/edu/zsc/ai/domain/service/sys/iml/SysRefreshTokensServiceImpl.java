package edu.zsc.ai.domain.service.sys.iml;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.config.sys.RefreshTokenProperties;
import edu.zsc.ai.domain.mapper.sys.SysRefreshTokensMapper;
import edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RevokeRefreshTokenBySessionRequest;
import edu.zsc.ai.domain.model.entity.sys.SysRefreshTokens;
import edu.zsc.ai.domain.model.enums.RefreshTokenStatusEnum;
import edu.zsc.ai.domain.service.sys.SysRefreshTokensService;
import edu.zsc.ai.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service implementation for sys_refresh_tokens operations
 *
 * @author zgq
 */
@Service
public class SysRefreshTokensServiceImpl extends ServiceImpl<SysRefreshTokensMapper, SysRefreshTokens>
        implements SysRefreshTokensService {

    @Autowired
    private RefreshTokenProperties refreshProps;

    @Override
    public String createAndStoreRefreshToken(CreateRefreshTokenRequest request) {
        String plain = CryptoUtil.randomToken();
        SysRefreshTokens entity = new SysRefreshTokens();
        entity.setUserId(request.getUserId());
        entity.setSessionId(request.getSessionId());
        entity.setTokenHash(CryptoUtil.sha256Hex(plain));
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshProps.getExpireSeconds()));
        entity.setRevoked(RefreshTokenStatusEnum.NOT_REVOKED.getValue());
        save(entity);
        return plain;
    }

    @Override
    public SysRefreshTokens verifyAndGet(String refreshTokenPlain) {
        SysRefreshTokens token = getOne(new LambdaQueryWrapper<SysRefreshTokens>()
                .eq(SysRefreshTokens::getTokenHash, CryptoUtil.sha256Hex(refreshTokenPlain))
                .eq(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.NOT_REVOKED.getValue()));
        if (token == null) {
            return null;
        } else if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        } else {
            return token;
        }
    }

    @Override
    public void revoke(String refreshTokenPlain) {
        lambdaUpdate()
                .eq(SysRefreshTokens::getTokenHash, CryptoUtil.sha256Hex(refreshTokenPlain))
                .set(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.REVOKED.getValue())
                .update();
    }

    @Override
    public void revokeAllBySessionId(RevokeRefreshTokenBySessionRequest request) {
        lambdaUpdate()
                .eq(SysRefreshTokens::getSessionId, request.getSessionId())
                .eq(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.NOT_REVOKED.getValue())
                .set(SysRefreshTokens::getRevoked, RefreshTokenStatusEnum.REVOKED.getValue())
                .update();
    }
}