package edu.zsc.ai.domain.service.sys.iml;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseConstant;
import edu.zsc.ai.domain.mapper.sys.SysUsersMapper;
import edu.zsc.ai.domain.model.dto.request.sys.UpdateUserRequest;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.service.sys.SysUsersService;
import edu.zsc.ai.util.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service implementation for sys_users operations
 *
 * @author zgq
 */
@Service
public class SysUsersServiceImpl extends ServiceImpl<SysUsersMapper, SysUsers>
                implements SysUsersService {

        @Override
        public Boolean updateUserProfile(UpdateUserRequest request) {
                // Get current user context from StpUtil
                long userId = StpUtil.getLoginIdAsLong();

                // Get current user
                SysUsers user = this.getById(userId);
                BusinessException.throwIf(user == null, ResponseConstant.UNAUTHORIZED,
                                ResponseConstant.USER_NOT_FOUND_MESSAGE);

                // Check if username is being changed and if it's already taken
                if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                        boolean usernameExists = this.exists(new LambdaQueryWrapper<SysUsers>()
                                        .eq(SysUsers::getUsername, request.getUsername())
                                        .ne(SysUsers::getId, userId));
                        BusinessException.throwIf(usernameExists, ResponseConstant.PARAM_ERROR,
                                        "Username already taken");
                }

                // Update only non-null fields
                boolean updated = false;
                if (request.getUsername() != null) {
                        user.setUsername(request.getUsername());
                        updated = true;
                }
                if (request.getAvatarUrl() != null) {
                        user.setAvatarUrl(request.getAvatarUrl());
                        updated = true;
                }

                if (updated) {
                        user.setUpdatedAt(LocalDateTime.now());
                        return this.updateById(user);
                }

                return true;
        }
}