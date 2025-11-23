package edu.zsc.ai.service.ai.manager;

import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import java.util.List;

public interface ConcurrencyManager {

    boolean tryLockConversation(Long conversationId);

    void unlockConversation(Long conversationId);

    void enqueueMessage(Long conversationId, ChatRequest request);

    List<ChatRequest> getAllQueuedMessages(Long conversationId);
}
