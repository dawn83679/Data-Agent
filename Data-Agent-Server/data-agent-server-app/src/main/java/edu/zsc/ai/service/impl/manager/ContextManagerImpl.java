package edu.zsc.ai.service.impl.manager;

import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.service.AiMessageService;
import edu.zsc.ai.service.manager.ContextManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManagerImpl implements ContextManager {

    private final AiMessageService aiMessageService;

    @Override
    public HistoryContextResponse getContextForAI(Long conversationId) {
        return aiMessageService.getAIContext(conversationId);
    }

    @Override
    public HistoryContextResponse compressContext(Long conversationId, HistoryContextResponse context, int maxTokens) {
        return context;
    }
}
