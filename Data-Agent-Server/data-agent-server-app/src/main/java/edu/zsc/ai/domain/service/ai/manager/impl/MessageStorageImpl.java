package edu.zsc.ai.domain.service.ai.manager.impl;

import edu.zsc.ai.common.enums.ai.message.MessageRoleEnum;
import edu.zsc.ai.domain.model.dto.request.ai.message.SaveMessageRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMessage;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import edu.zsc.ai.domain.service.ai.manager.MessageStorage;

@Component
@RequiredArgsConstructor
public class MessageStorageImpl implements MessageStorage {

    private final AiMessageService aiMessageService;

    @Override
    public Long saveUserMessage(Long conversationId, String content) {
        SaveMessageRequest request = new SaveMessageRequest();
        request.setConversationId(conversationId);
        request.setRole(MessageRoleEnum.USER.name());
        request.setContent(content);
        AiMessage savedMessage = aiMessageService.saveMessage(request);
        return savedMessage.getId();
    }

    @Override
    public Long saveAiMessage(Long conversationId, String content) {
        SaveMessageRequest request = new SaveMessageRequest();
        request.setConversationId(conversationId);
        request.setRole(MessageRoleEnum.ASSISTANT.name());
        request.setContent(content);
        AiMessage savedMessage = aiMessageService.saveMessage(request);
        return savedMessage.getId();
    }

    @Override
    public int rollbackMessages(Long conversationId, Long rollbackToMessageId) {
        return aiMessageService.rollbackMessages(conversationId, rollbackToMessageId);
    }
}
