package edu.zsc.ai.domain.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.sys.CreateRefreshTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RevokeRefreshTokenBySessionRequest;
import edu.zsc.ai.domain.model.entity.sys.SysRefreshTokens;

public interface SysRefreshTokensService extends IService<SysRefreshTokens> {

    String createAndStoreRefreshToken(CreateRefreshTokenRequest request);

    SysRefreshTokens verifyAndGet(String refreshTokenPlain);

    void revoke(String refreshTokenPlain);

    void revokeAllBySessionId(RevokeRefreshTokenBySessionRequest request);
}