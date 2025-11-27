package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * Session Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
