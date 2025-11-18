package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.SysUsers;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for sys_users table
 *
 * @author zgq
 */
@Mapper
public interface SysUsersMapper extends BaseMapper<SysUsers> {
}