package edu.zsc.ai.domain.service.ai;

import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;

public interface MemoryContextService {

    /**
     * Loads the memory payload used when assembling the runtime user prompt.
     * Returns an empty payload if memory is disabled or parameters are null.
     */
    MemoryPromptContext loadPromptContext(Long userId, Long conversationId, String userMessage);
}
