package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.ai.AiMessageBlockMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.domain.service.ai.AiMessageBlockService;
import org.springframework.stereotype.Service;

@Service
public class AiMessageBlockServiceImpl extends ServiceImpl<AiMessageBlockMapper, AiMessageBlock>
        implements AiMessageBlockService {

}
