package edu.zsc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.SysRefreshTokensMapper;
import edu.zsc.ai.model.entity.SysRefreshTokens;
import edu.zsc.ai.service.SysRefreshTokensService;
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