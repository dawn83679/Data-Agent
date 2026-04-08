package edu.zsc.ai.domain.service.ai;

import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;

public interface MemoryContextService {

    /**
<<<<<<< HEAD
     * Loads the durable-memory payload projected into the runtime prompt suffix.
=======
     * Loads the memory payload used when assembling the runtime user prompt.
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
     * Returns an empty payload if memory is disabled or parameters are null.
     */
    MemoryPromptContext loadPromptContext(Long userId, Long conversationId, String userMessage);
}
