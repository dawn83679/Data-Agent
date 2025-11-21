package edu.zsc.ai.service.impl.manager;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.model.entity.ai.AiConversation;
import edu.zsc.ai.service.AiConversationService;
import edu.zsc.ai.service.manager.ConversationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * @author zgq
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationManagerImpl implements ConversationManager {

    private final AiConversationService aiConversationService;

    @Override
    public Long createOrGetConversation(Long conversationId, String title) {
        return aiConversationService.createOrGetConversation(conversationId, StpUtil.getLoginIdAsLong(), title);
    }

    @Override
    public Integer getCurrentTokens(Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        AiConversation conversation = aiConversationService.getByIdAndUser(conversationId, userId);
       
        return conversation.getTokenCount();
    }

    @Override
    public void updateConversationTokens(AiConversation conversation) {
        Long userId = StpUtil.getLoginIdAsLong();
        aiConversationService.updateConversationTokens(conversation.getId(), userId, conversation.getTokenCount());
    }

}

