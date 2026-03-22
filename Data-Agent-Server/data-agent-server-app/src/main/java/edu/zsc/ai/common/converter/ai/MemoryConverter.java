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
                .scope(memory.getScope())
                .memoryType(memory.getMemoryType())
                .subType(memory.getSubType())
                .sourceType(memory.getSourceType())
                .title(memory.getTitle())
                .content(memory.getContent())
                .reason(memory.getReason())
                .enable(memory.getEnable())
                .accessCount(memory.getAccessCount())
                .lastAccessedAt(memory.getLastAccessedAt())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
