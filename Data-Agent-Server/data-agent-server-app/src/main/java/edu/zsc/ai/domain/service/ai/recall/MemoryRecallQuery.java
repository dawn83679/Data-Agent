package edu.zsc.ai.domain.service.ai.recall;

public record MemoryRecallQuery(
        String queryName,
        String planningReason,
        String targetScope,
        Long conversationId,
        String queryText,
        String memoryType,
        String subType,
        Double minScore,
        MemoryRecallMode recallMode,
        MemoryRecallQueryStrategy queryStrategy,
        int priority
) {
}
