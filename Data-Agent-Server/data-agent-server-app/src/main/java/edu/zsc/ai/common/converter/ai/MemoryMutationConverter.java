package edu.zsc.ai.common.converter.ai;

import java.time.LocalDateTime;

import edu.zsc.ai.common.enums.ai.MemoryEnableEnum;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;

public final class MemoryMutationConverter {

    private MemoryMutationConverter() {
    }

    /**
     * Builds a new {@link AiMemory} row for agent-driven auto-write (same fields as the inline builder in
     * {@code MemoryServiceImpl#createAutoMemory}).
     */
    public static AiMemory buildAutoWriteCreate(
            Long userId,
            Long conversationId,
            String scope,
            String memoryType,
            String subType,
            String sourceType,
            String title,
            String content,
            String reason,
            LocalDateTime now) {
        return AiMemory.builder()
                .userId(userId)
                .conversationId(conversationId)
                .scope(scope)
                .memoryType(memoryType)
                .subType(subType)
                .sourceType(sourceType)
                .title(title)
                .content(content)
                .reason(reason)
                .enable(MemoryEnableEnum.ENABLE.getCode())
                .accessCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static AiMemory create(Long userId, Mutation mutation) {
        return AiMemory.builder()
                .userId(userId)
                .conversationId(mutation.conversationId())
                .scope(mutation.scope())
                .memoryType(mutation.memoryType())
                .subType(mutation.subType())
                .sourceType(mutation.sourceType())
                .title(mutation.title())
                .content(mutation.content())
                .reason(mutation.reason())
                .enable(mutation.enable())
                .accessCount(mutation.accessCount())
                .createdAt(mutation.now())
                .updatedAt(mutation.now())
                .build();
    }

    public static AiMemory apply(AiMemory memory, Mutation mutation) {
        memory.setConversationId(mutation.conversationId());
        memory.setScope(mutation.scope());
        memory.setMemoryType(mutation.memoryType());
        memory.setSubType(mutation.subType());
        memory.setSourceType(mutation.sourceType());
        memory.setTitle(mutation.title());
        memory.setContent(mutation.content());
        memory.setReason(mutation.reason());
        memory.setUpdatedAt(mutation.now());
        return memory;
    }

    public record Mutation(
            Long conversationId,
            String scope,
            String memoryType,
            String subType,
            String sourceType,
            String title,
            String content,
            String reason,
            Integer enable,
            Integer accessCount,
            LocalDateTime now) {
    }
}
