package edu.zsc.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import edu.zsc.ai.model.entity.Session;

/**
 * Session Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
