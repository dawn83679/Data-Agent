package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;

import java.util.List;

public interface AiMessageService extends IService<StoredChatMessage> {

    List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId);

    void saveBatchMessages(List<StoredChatMessage> messages);

    int removeByConversationId(Long conversationId);
}
