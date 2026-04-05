package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;

import java.util.List;

/**
 * Calls AI to extract memory write items from a conversation slice.
 */
public interface ConversationMemoryAiWriter {

    /**
     * Analyzes the conversation context and returns memory write instructions.
     *
     * @param context new messages + existing memory summaries
     * @return list of write items (may be empty)
     */
    List<MemoryWriteItem> extractMemoryWrites(MemoryWriteContext context);
}
