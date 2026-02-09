package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.enums.ai.MessageStatusEnum;
import edu.zsc.ai.domain.mapper.ai.AiMessageMapper;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.service.ai.AiMessageBlockService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, CustomAiMessage>
        implements AiMessageService {

    private final AiMessageBlockService aiMessageBlockService;

    @Override
    public List<CustomAiMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        LambdaQueryWrapper<CustomAiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomAiMessage::getConversationId, conversationId)
                .eq(CustomAiMessage::getStatus, MessageStatusEnum.NORMAL.getCode())
                .orderByAsc(CustomAiMessage::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public void saveBatchMessages(List<CustomAiMessage> messages) {
         saveBatch(messages);
    }

    @Override
    @Transactional
    public int removeByConversationId(Long conversationId) {
        List<CustomAiMessage> messages = getByConversationIdOrderByCreatedAtAsc(conversationId);
        if (messages.isEmpty()) {
            return 0;
        }

        List<Long> messageIds = messages.stream()
                .map(CustomAiMessage::getId)
                .toList();

        // Delete associated blocks first
        aiMessageBlockService.deleteByMessageIds(messageIds);

        // Delete messages
        removeByIds(messageIds);

        log.debug("Deleted {} messages and {} blocks for conversation {}",
                messageIds.size(), messageIds.size(), conversationId);

        return messageIds.size();
    }

    @Override
    @Transactional
    public int removeAllByConversationId(Long conversationId) {
        LambdaQueryWrapper<CustomAiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomAiMessage::getConversationId, conversationId);
        List<CustomAiMessage> messages = list(wrapper);
        if (messages.isEmpty()) {
            return 0;
        }
        List<Long> messageIds = messages.stream()
                .map(CustomAiMessage::getId)
                .toList();
        aiMessageBlockService.deleteByMessageIds(messageIds);
        removeByIds(messageIds);
        log.debug("Deleted all {} messages for conversation {}", messageIds.size(), conversationId);
        return messageIds.size();
    }
}
