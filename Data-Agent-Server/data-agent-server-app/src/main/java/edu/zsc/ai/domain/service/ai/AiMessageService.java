package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.langchain4j.data.message.ChatMessage;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;

import java.util.List;

public interface AiMessageService extends IService<StoredChatMessage> {

    List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<StoredChatMessage> getActiveByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<StoredChatMessage> getActiveMessagesForAutoWrite(Long conversationId);

    List<StoredChatMessage> getMessagesForAutoWriteAfter(Long conversationId, Long lastProcessedMessageId);

    void saveBatchMessages(List<StoredChatMessage> messages);

    int removeByConversationId(Long conversationId);

    /**
     * Replaces all messages for a conversation: removes existing ones and persists new ones.
     * Handles user-message normalization and compression-status marking.
     *
     * @param conversationId conversation ID
     * @param messages       the ChatMessages to persist
     */
    void replaceConversationMessages(Long conversationId, List<ChatMessage> messages);

    /**
     * Updates the token count of the last AI message in a conversation.
     * Used to persist token usage after streaming chat completes.
     *
     * @param conversationId conversation ID
     * @param tokenCount     output token count for the AI response (from response.tokenUsage().outputTokenCount())
     */
    void updateLastAiMessageTokenCount(Long conversationId, Integer tokenCount);
}
