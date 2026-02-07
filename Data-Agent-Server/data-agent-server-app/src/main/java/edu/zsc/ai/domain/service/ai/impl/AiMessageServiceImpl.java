package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.ai.AiMessageMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMessage;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import org.springframework.stereotype.Service;

@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService {
}
