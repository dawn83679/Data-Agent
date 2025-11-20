package edu.zsc.ai.service.manager;

import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;

public interface ContextManager {

    HistoryContextResponse getContextForAI(Long conversationId);

    HistoryContextResponse compressContext(Long conversationId, HistoryContextResponse context, int maxTokens);
}
