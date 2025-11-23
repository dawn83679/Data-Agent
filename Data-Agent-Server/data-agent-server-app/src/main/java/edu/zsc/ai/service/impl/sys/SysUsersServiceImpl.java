package edu.zsc.ai.service.impl.sys;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.sys.SysUsersMapper;
import edu.zsc.ai.model.entity.sys.SysUsers;
import edu.zsc.ai.service.sys.SysUsersService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for sys_users operations
 *
 * @author zgq
 */
@Service
public class SysUsersServiceImpl extends ServiceImpl<SysUsersMapper, SysUsers>
        implements SysUsersService {
}