package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.domain.mapper.ai.AiConversationMapper;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import edu.zsc.ai.common.converter.ai.StoredMessageToResponseConverter;
import edu.zsc.ai.domain.model.dto.response.ai.ConversationMessageResponse;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    private final AiMessageService aiMessageService;
    private final StoredMessageToResponseConverter messageConverter;

    private long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public void checkAccess(Long userId, Long conversationId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId);
        boolean exists = count(wrapper) > 0;

        BusinessException.assertTrue(exists, ResponseMessageKey.FORBIDDEN_MESSAGE);
    }

    @Override
    public AiConversation createConversation(Long userId, String title) {
        // TODO: Use AI to summarize the user's first message and generate a concise title instead of truncating
        // For now, truncate to 100 characters to prevent database varchar(255) overflow
        String truncatedTitle = title;
        if (title != null && title.length() > 100) {
            truncatedTitle = title.substring(0, 100) + "...";
        }

        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title(truncatedTitle)
                .tokenCount(0)
                .build();

        save(conversation);

        return conversation;
    }

    @Override
    public Page<AiConversation> pageByCurrentUser(PageRequest pageRequest) {
        long userId = getCurrentUserId();
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getUpdatedAt);
        Page<AiConversation> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        return page(page, wrapper);
    }

    @Override
    public AiConversation getByIdForCurrentUser(Long conversationId) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        AiConversation one = getById(conversationId);
        BusinessException.assertNotNull(one, ResponseCode.FORBIDDEN, ResponseMessageKey.FORBIDDEN_MESSAGE);
        return one;
    }

    @Override
    public AiConversation updateTitle(Long conversationId, String title) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        LambdaUpdateWrapper<AiConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .set(AiConversation::getTitle, title)
                .set(AiConversation::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
        return getById(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCurrentUser(Long conversationId) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        aiMessageService.removeByConversationId(conversationId);
        removeById(conversationId);
        log.info("Deleted conversation {} for user {}", conversationId, userId);
    }

    @Override
    public List<ConversationMessageResponse> getMessagesForCurrentUser(Long conversationId) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        List<StoredChatMessage> stored = aiMessageService.getByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ConversationMessageResponse> result = new ArrayList<>(stored.size());
        for (StoredChatMessage s : stored) {
            try {
                ChatMessage message = ChatMessageDeserializer.messageFromJson(s.getData());
                result.add(messageConverter.toResponse(s, message));
            } catch (Exception e) {
                log.warn("Failed to deserialize message id={}, skipping", s.getId(), e);
            }
        }
        return result;
    }

    @Override
    public void updateTokenCount(Long conversationId, Integer tokenCount) {
        if (conversationId == null || tokenCount == null || tokenCount <= 0) {
            return;
        }

        // Update token_count
        LambdaUpdateWrapper<AiConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .set(AiConversation::getTokenCount, tokenCount)
                .set(AiConversation::getUpdatedAt, LocalDateTime.now());

        boolean updated = update(wrapper);
        if (!updated) {
            log.warn("Failed to update token count for conversation {}, conversation may not exist", conversationId);
        }
    }
}
