package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;

import java.util.List;

public interface AiMessageService extends IService<StoredChatMessage> {

    List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId);

    void saveBatchMessages(List<StoredChatMessage> messages);

    int removeByConversationId(Long conversationId);

    /**
     * Updates the token count of the last AI message in a conversation.
     * Used to persist token usage after streaming chat completes.
     *
     * @param conversationId conversation ID
     * @param tokenCount     output token count for the AI response (from response.tokenUsage().outputTokenCount())
     */
    void updateLastAiMessageTokenCount(Long conversationId, Integer tokenCount);
}
