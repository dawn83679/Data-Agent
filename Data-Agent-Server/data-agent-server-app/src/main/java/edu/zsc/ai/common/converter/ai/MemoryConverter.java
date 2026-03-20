package edu.zsc.ai.common.converter.ai;

import edu.zsc.ai.domain.model.dto.response.ai.MemoryResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;

public final class MemoryConverter {

    private MemoryConverter() {
    }

    public static MemoryResponse toMemoryResponse(AiMemory memory) {
        if (memory == null) {
            return null;
        }
        return MemoryResponse.builder()
                .id(memory.getId())
                .conversationId(memory.getConversationId())
                .workspaceContextKey(memory.getWorkspaceContextKey())
                .workspaceLevel(memory.getWorkspaceLevel())
                .scope(memory.getScope())
                .memoryType(memory.getMemoryType())
                .subType(memory.getSubType())
                .sourceType(memory.getSourceType())
                .title(memory.getTitle())
                .content(memory.getContent())
                .normalizedContentKey(memory.getNormalizedContentKey())
                .reason(memory.getReason())
                .sourceMessageIds(memory.getSourceMessageIds())
                .detailJson(memory.getDetailJson())
                .status(memory.getStatus())
                .confidenceScore(memory.getConfidenceScore())
                .salienceScore(memory.getSalienceScore())
                .accessCount(memory.getAccessCount())
                .useCount(memory.getUseCount())
                .lastAccessedAt(memory.getLastAccessedAt())
                .lastUsedAt(memory.getLastUsedAt())
                .expiresAt(memory.getExpiresAt())
                .archivedAt(memory.getArchivedAt())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
