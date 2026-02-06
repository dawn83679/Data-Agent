package edu.zsc.ai.domain.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AiTodoTaskMapper
 * Mapper interface for ai_todo_task table.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Mapper
public interface AiTodoTaskMapper extends BaseMapper<AiTodoTask> {
}
