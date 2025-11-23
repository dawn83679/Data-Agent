package edu.zsc.ai.service.impl.sys;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.sys.SysSessionsMapper;
import edu.zsc.ai.model.entity.sys.SysSessions;
import edu.zsc.ai.service.sys.SysSessionsService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for sys_sessions operations
 *
 * @author zgq
 */
@Service
public class SysSessionsServiceImpl extends ServiceImpl<SysSessionsMapper, SysSessions>
        implements SysSessionsService {
}