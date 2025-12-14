package edu.zsc.ai.domain.service.sys.iml;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.sys.SysSessionsMapper;
import edu.zsc.ai.domain.model.dto.request.sys.FindSessionByTokenRequest;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;
import edu.zsc.ai.domain.service.sys.SysSessionsService;
import edu.zsc.ai.util.CryptoUtil;
import org.springframework.stereotype.Service;

/**
 * Service implementation for sys_sessions operations
 *
 * @author zgq
 */
@Service
public class SysSessionsServiceImpl extends ServiceImpl<SysSessionsMapper, SysSessions>
        implements SysSessionsService {

    @Override
    public SysSessions findByAccessTokenAndUserId(FindSessionByTokenRequest request)  {
        String accessTokenHash = CryptoUtil.sha256Hex(request.getAccessToken());
        return getOne(new LambdaQueryWrapper<SysSessions>()
                .eq(SysSessions::getAccessTokenHash, accessTokenHash)
                .eq(SysSessions::getUserId, request.getUserId()));
    }
}