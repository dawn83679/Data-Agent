package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.AiMessageBlockMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.domain.service.ai.AiMessageBlockService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_message_block operations
 *
 * @author zgq
 */
@Service
public class AiMessageBlockServiceImpl extends ServiceImpl<AiMessageBlockMapper, AiMessageBlock>
        implements AiMessageBlockService {
}