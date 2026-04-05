package edu.zsc.ai.domain.service.ai.model;

import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Context for auto memory write: new messages + existing memory summaries for dedup.
 */
public record MemoryWriteContext(
        List<StoredChatMessage> newMessages,
        List<MemorySummary> existingMemories,
        boolean memoriesTruncated,
        int totalEnabledCount
) {

    public boolean isEmpty() {
        return newMessages == null || newMessages.isEmpty();
    }

    public boolean hasUserMessage() {
        return newMessages != null && newMessages.stream()
                .anyMatch(m -> "USER".equals(m.getRole()));
    }

    public Long lastMessageId() {
        if (isEmpty()) {
            return null;
        }
        return newMessages.get(newMessages.size() - 1).getId();
    }

    /**
     * Summary of an existing memory, used as AI input for dedup decisions.
     */
    public record MemorySummary(
            Long memoryId,
            String scope,
            String memoryType,
            String subType,
            String title,
            String contentPreview,
            LocalDateTime updatedAt
    ) {
        public String toPromptLine() {
            return String.format("[%s/%s #%d] %s — %s (updated: %s)",
                    memoryType, subType, memoryId, title,
                    contentPreview, updatedAt != null ? updatedAt.toLocalDate() : "unknown");
        }
    }
}
