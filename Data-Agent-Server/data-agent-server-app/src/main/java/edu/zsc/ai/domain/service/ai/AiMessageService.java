package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;

import java.util.List;

public interface AiMessageService extends IService<CustomAiMessage> {

    List<CustomAiMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId);


    void saveBatchMessages(List<CustomAiMessage> messages);


    int removeByConversationId(Long conversationId);

    /**
     * Remove all messages (any status) and their blocks for a conversation.
     * Used when deleting a conversation.
     *
     * @param conversationId conversation ID
     * @return number of messages removed
     */
    int removeAllByConversationId(Long conversationId);
}
