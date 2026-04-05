package edu.zsc.ai.domain.service.ai;

import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;

public interface MemoryContextService {

    /**
     * Loads the durable-memory payload projected into the runtime prompt suffix.
     * Returns an empty payload if memory is disabled or parameters are null.
     */
    MemoryPromptContext loadPromptContext(Long userId, Long conversationId, String userMessage);
}
