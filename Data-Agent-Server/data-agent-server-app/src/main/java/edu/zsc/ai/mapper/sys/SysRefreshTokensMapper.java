package edu.zsc.ai.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.sys.SysRefreshTokens;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for sys_refresh_tokens table
 *
 * @author zgq
 */
@Mapper
public interface SysRefreshTokensMapper extends BaseMapper<SysRefreshTokens> {
}