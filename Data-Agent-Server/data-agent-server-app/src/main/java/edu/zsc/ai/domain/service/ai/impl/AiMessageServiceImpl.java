package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.ai.AiMessageMapper;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, StoredChatMessage>
        implements AiMessageService {

    @Override
    public List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .orderByAsc(StoredChatMessage::getCreatedAt)
                .orderByAsc(StoredChatMessage::getId);
        return list(wrapper);
    }

    @Override
    public void saveBatchMessages(List<StoredChatMessage> messages) {
        saveBatch(messages);
    }

    @Override
    @Transactional
    public int removeByConversationId(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId);
        int count = (int) count(wrapper);
        if (count == 0) {
            return 0;
        }
        remove(wrapper);
        log.debug("Deleted {} messages for conversation {}", count, conversationId);
        return count;
    }
}
