package edu.zsc.ai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_todo_task table
 *
 * @author zgq
 */
@Mapper
public interface AiTodoTaskMapper extends BaseMapper<AiTodoTask> {
}