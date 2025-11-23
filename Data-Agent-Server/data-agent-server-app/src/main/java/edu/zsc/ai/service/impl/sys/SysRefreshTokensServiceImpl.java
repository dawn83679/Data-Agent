package edu.zsc.ai.service.impl.sys;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.sys.SysRefreshTokensMapper;
import edu.zsc.ai.model.entity.sys.SysRefreshTokens;
import edu.zsc.ai.service.sys.SysRefreshTokensService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for sys_refresh_tokens operations
 *
 * @author zgq
 */
@Service
public class SysRefreshTokensServiceImpl extends ServiceImpl<SysRefreshTokensMapper, SysRefreshTokens>
        implements SysRefreshTokensService {
}