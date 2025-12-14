package edu.zsc.ai.domain.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.sys.FindSessionByTokenRequest;
import edu.zsc.ai.domain.model.entity.sys.SysSessions;

/**
 * Service interface for sys_sessions operations
 *
 * @author zgq
 */
public interface SysSessionsService extends IService<SysSessions> {
    /**
     * Find session by accessToken and user ID
     */
    SysSessions findByAccessTokenAndUserId(FindSessionByTokenRequest request);
}