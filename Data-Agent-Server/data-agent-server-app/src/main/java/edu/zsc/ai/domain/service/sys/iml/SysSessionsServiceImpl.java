package edu.zsc.ai.domain.service.sys.iml;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseConstant;
import edu.zsc.ai.common.enums.sys.SessionStatusEnum;
import edu.zsc.ai.domain.mapper.sys.SysSessionsMapper;
import edu.zsc.ai.domain.model.dto.request.sys.FindSessionByTokenRequest;
import edu.zsc.ai.domain.model.dto.request.sys.RevokeRefreshTokenBySessionRequest;
import edu.zsc.ai.domain.model.dto.response.sys.SessionResponse;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.service.sys.SysRefreshTokensService;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.util.CryptoUtil;
import edu.zsc.ai.util.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for sys_sessions operations
 *
 * @author zgq
 */
@Service
public class SysSessionsServiceImpl extends ServiceImpl<SysSessionsMapper, SysSessions>
                implements SysSessionsService {

        @Autowired
        private SysRefreshTokensService sysRefreshTokensService;

        @Override
        public SysSessions findByAccessTokenAndUserId(FindSessionByTokenRequest request) {
                String accessTokenHash = CryptoUtil.sha256Hex(request.getAccessToken());
                return getOne(new LambdaQueryWrapper<SysSessions>()
                                .eq(SysSessions::getAccessTokenHash, accessTokenHash)
                                .eq(SysSessions::getUserId, request.getUserId()));
        }

        @Override
        public List<SessionResponse> listActiveSessionsByUserId() {
                // Get current user context from StpUtil
                long userId = StpUtil.getLoginIdAsLong();
                String currentAccessToken = StpUtil.getTokenValue();
                String currentAccessTokenHash = CryptoUtil.sha256Hex(currentAccessToken);

                List<SysSessions> sessions = list(new LambdaQueryWrapper<SysSessions>()
                                .eq(SysSessions::getUserId, userId)
                                .eq(SysSessions::getActive, SessionStatusEnum.ACTIVE.getValue())
                                .orderByDesc(SysSessions::getLastRefreshAt));

                return sessions.stream()
                                .map(session -> SessionResponse.builder()
                                                .id(session.getId())
                                                .deviceInfo(session.getDeviceInfo())
                                                .ipAddress(session.getIpAddress())
                                                .userAgent(session.getUserAgent())
                                                .isCurrent(session.getAccessTokenHash().equals(currentAccessTokenHash))
                                                .lastRefreshAt(session.getLastRefreshAt())
                                                .createdAt(session.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public Boolean revokeSessionById(Long sessionId) {
                // Get current user context from StpUtil
                long userId = StpUtil.getLoginIdAsLong();

                // Get session and verify ownership
                SysSessions session = getById(sessionId);
                BusinessException.throwIf(session == null, ResponseConstant.PARAM_ERROR,
                                "Session not found");
                BusinessException.throwIf(!session.getUserId().equals(userId), ResponseConstant.UNAUTHORIZED,
                                "Not authorized to revoke this session");

                // Mark session as inactive
                session.setActive(SessionStatusEnum.INACTIVE.getValue());
                boolean updated = updateById(session);

                // Revoke all refresh tokens associated with this session
                if (updated) {
                        RevokeRefreshTokenBySessionRequest revokeReq = new RevokeRefreshTokenBySessionRequest();
                        revokeReq.setSessionId(sessionId);
                        sysRefreshTokensService.revokeAllBySessionId(revokeReq);
                }

                return updated;
        }
}