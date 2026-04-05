package edu.zsc.ai.domain.service.ai.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.domain.mapper.ai.AiConversationMemoryCursorMapper;
import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;
import edu.zsc.ai.domain.service.ai.AiConversationMemoryCursorService;

@Service
public class AiConversationMemoryCursorServiceImpl
        extends ServiceImpl<AiConversationMemoryCursorMapper, AiConversationMemoryCursor>
        implements AiConversationMemoryCursorService {
}
