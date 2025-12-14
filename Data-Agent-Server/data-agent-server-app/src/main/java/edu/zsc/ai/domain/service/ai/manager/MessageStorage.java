package edu.zsc.ai.domain.service.ai.manager;

public interface MessageStorage {

    Long saveUserMessage(Long conversationId, String content);

    Long saveAiMessage(Long conversationId, String content);

    int rollbackMessages(Long conversationId, Long rollbackToMessageId);
}
