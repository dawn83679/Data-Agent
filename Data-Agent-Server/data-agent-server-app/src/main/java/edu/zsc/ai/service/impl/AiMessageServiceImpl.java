package edu.zsc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.AiMessageMapper;
import edu.zsc.ai.model.entity.AiMessage;
import edu.zsc.ai.service.AiMessageService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_message operations
 *
 * @author zgq
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService {
}