package edu.zsc.ai.domain.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.sys.UpdateUserRequest;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;

/**
 * Service interface for sys_users operations
 *
 * @author zgq
 */
public interface SysUsersService extends IService<SysUsers> {
    /**
     * Update user profile
     */
    Boolean updateUserProfile(UpdateUserRequest request);
}