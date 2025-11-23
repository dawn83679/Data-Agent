package edu.zsc.ai.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.dto.request.ai.message.MessageQueryRequest;
import edu.zsc.ai.model.dto.request.ai.message.SaveMessageRequest;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.model.dto.response.base.PageResponse;
import edu.zsc.ai.model.entity.ai.AiMessage;

/**
 * @author zgq
 */
public interface AiMessageService extends IService<AiMessage> {

    AiMessage saveMessage(SaveMessageRequest request);

    HistoryContextResponse getAIContext(Long conversationId);


    PageResponse<AiMessage> getDisplayMessagesPaginated(MessageQueryRequest request);


    int rollbackMessages(Long conversationId, Long rollbackToMessageId);
}