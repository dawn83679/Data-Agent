package edu.zsc.ai.domain.service.sys.iml;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.sys.SysUsersMapper;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.service.sys.SysUsersService;
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