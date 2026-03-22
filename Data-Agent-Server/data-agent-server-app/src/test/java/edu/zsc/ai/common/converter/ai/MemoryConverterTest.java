package edu.zsc.ai.common.converter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.domain.model.dto.response.ai.MemoryResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;

class MemoryConverterTest {

    @Test
    void toMemoryResponse_mapsExpandedMemoryFields() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 18, 12, 30);
        AiMemory memory = AiMemory.builder()
                .id(3L)
                .conversationId(9L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .sourceType("MANUAL")
                .title("Concise output")
                .content("User prefers concise output.")
                .enable(1)
                .accessCount(4)
                .lastAccessedAt(now)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();

        MemoryResponse response = MemoryConverter.toMemoryResponse(memory);

        assertEquals(memory.getId(), response.getId());
        assertEquals(memory.getConversationId(), response.getConversationId());
        assertEquals(memory.getMemoryType(), response.getMemoryType());
        assertEquals(memory.getContent(), response.getContent());
        assertEquals(memory.getAccessCount(), response.getAccessCount());
        assertEquals(memory.getUpdatedAt(), response.getUpdatedAt());
    }
}
