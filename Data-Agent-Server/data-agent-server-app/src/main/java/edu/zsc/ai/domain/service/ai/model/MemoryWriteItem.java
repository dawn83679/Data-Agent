package edu.zsc.ai.domain.service.ai.model;

/**
 * A single memory write instruction returned by the AI auto-writer.
 */
public record MemoryWriteItem(
        String operation,
        Long memoryId,
        String scope,
        String memoryType,
        String subType,
        String title,
        String content,
        String reason
) {
}
