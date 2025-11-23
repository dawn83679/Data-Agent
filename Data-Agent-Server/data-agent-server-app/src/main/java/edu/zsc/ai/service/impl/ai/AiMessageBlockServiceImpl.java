package edu.zsc.ai.service.impl.ai;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.AiMessageBlockMapper;
import edu.zsc.ai.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.service.ai.AiMessageBlockService;
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