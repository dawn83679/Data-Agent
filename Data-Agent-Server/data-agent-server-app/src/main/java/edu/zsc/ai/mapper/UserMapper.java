package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
