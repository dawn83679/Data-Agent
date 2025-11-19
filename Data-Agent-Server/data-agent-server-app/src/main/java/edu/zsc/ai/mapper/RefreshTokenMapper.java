package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * Refresh Token Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
