package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;

import java.util.List;

public interface AiMessageService extends IService<CustomAiMessage> {

    List<CustomAiMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId);


    void saveBatchMessages(List<CustomAiMessage> messages);


    int removeByConversationId(Long conversationId);
}
