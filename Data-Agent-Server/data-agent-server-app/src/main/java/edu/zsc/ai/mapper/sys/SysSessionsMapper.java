package edu.zsc.ai.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.sys.SysSessions;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for sys_sessions table
 *
 * @author zgq
 */
@Mapper
public interface SysSessionsMapper extends BaseMapper<SysSessions> {
}