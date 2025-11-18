package edu.zsc.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.AiConversationMapper;
import edu.zsc.ai.model.entity.AiConversation;
import edu.zsc.ai.service.AiConversationService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_conversation operations
 *
 * @author zgq
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {
}