package edu.zsc.ai.domain.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for sys_users table
 *
 * @author zgq
 */
@Mapper
public interface SysUsersMapper extends BaseMapper<SysUsers> {
}