package edu.zsc.ai.domain.service.ai.manager;

import edu.zsc.ai.domain.model.dto.response.ai.message.HistoryContextResponse;

public interface ContextManager {

    HistoryContextResponse getContextForAI(Long conversationId);

    HistoryContextResponse compressContext(Long conversationId, HistoryContextResponse context);
}
