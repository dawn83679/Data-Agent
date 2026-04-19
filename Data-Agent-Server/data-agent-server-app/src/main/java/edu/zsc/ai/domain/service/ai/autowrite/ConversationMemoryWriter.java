package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;

/**
 * Internal background writer that updates conversation memory through readMemory/updateMemory tools.
 */
public interface ConversationMemoryWriter {

    void writeMemory(MemoryWriteContext context, Long conversationId, Long userId);
}
