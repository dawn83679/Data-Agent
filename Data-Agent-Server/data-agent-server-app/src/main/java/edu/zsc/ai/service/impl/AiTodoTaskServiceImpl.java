package edu.zsc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.AiTodoTaskMapper;
import edu.zsc.ai.model.entity.AiTodoTask;
import edu.zsc.ai.service.AiTodoTaskService;
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