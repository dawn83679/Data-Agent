package edu.zsc.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import edu.zsc.ai.model.entity.LoginLog;

/**
 * Login Log Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
