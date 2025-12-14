package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.AiTodoTaskMapper;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;
import edu.zsc.ai.domain.service.ai.AiTodoTaskService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_todo_task operations
 *
 * @author zgq
 */
@Service
public class AiTodoTaskServiceImpl extends ServiceImpl<AiTodoTaskMapper, AiTodoTask>
        implements AiTodoTaskService {
}