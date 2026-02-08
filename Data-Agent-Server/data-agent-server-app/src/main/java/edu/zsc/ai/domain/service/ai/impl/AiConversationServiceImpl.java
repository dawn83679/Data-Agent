package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.domain.mapper.ai.AiConversationMapper;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    @Override
    public void checkAccess(Long userId, Long conversationId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId);
        boolean exists = count(wrapper) > 0;

        BusinessException.assertTrue(exists, ResponseMessageKey.FORBIDDEN_MESSAGE);
    }
}
