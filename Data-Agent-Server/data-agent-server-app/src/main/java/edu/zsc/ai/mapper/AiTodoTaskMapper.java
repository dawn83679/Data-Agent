package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.ai.AiTodoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_todo_task table
 *
 * @author zgq
 */
@Mapper
public interface AiTodoTaskMapper extends BaseMapper<AiTodoTask> {
}