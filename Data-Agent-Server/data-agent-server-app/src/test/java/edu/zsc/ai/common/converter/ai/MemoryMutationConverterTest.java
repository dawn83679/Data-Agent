package edu.zsc.ai.common.converter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.domain.model.entity.ai.AiMemory;

class MemoryMutationConverterTest {

    @Test
    void create_buildsNewMemoryFromMutation() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 22, 16, 0);
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                7L,
                "USER",
                "PREFERENCE",
                "LANGUAGE_PREFERENCE",
                "AGENT",
                "语言偏好",
                "用户偏好使用中文回答。",
                "用户明确要求后续默认用中文回答。",
                1,
                0,
                now
        );

        AiMemory memory = MemoryMutationConverter.create(42L, mutation);

        assertEquals(42L, memory.getUserId());
        assertEquals(7L, memory.getConversationId());
        assertEquals("USER", memory.getScope());
        assertEquals("PREFERENCE", memory.getMemoryType());
        assertEquals("LANGUAGE_PREFERENCE", memory.getSubType());
        assertEquals("AGENT", memory.getSourceType());
        assertEquals("语言偏好", memory.getTitle());
        assertEquals("用户偏好使用中文回答。", memory.getContent());
        assertEquals("用户明确要求后续默认用中文回答。", memory.getReason());
        assertEquals(1, memory.getEnable());
        assertEquals(0, memory.getAccessCount());
        assertEquals(now, memory.getCreatedAt());
        assertEquals(now, memory.getUpdatedAt());
    }

    @Test
    void apply_overwritesExistingMemoryFromMutation() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 22, 16, 30);
        AiMemory memory = AiMemory.builder()
                .id(9L)
                .userId(42L)
                .conversationId(5L)
                .scope("CONVERSATION")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("PROCESS_RULE")
                .sourceType("MANUAL")
                .title("旧标题")
                .content("旧内容")
                .reason("旧原因")
                .enable(1)
                .accessCount(3)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .build();
        MemoryMutationConverter.Mutation mutation = new MemoryMutationConverter.Mutation(
                7L,
                "USER",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                "AGENT",
                "新标题",
                "用户偏好简洁回答。",
                "用户再次强调简洁输出。",
                1,
                0,
                now
        );

        MemoryMutationConverter.apply(memory, mutation);

        assertEquals(9L, memory.getId());
        assertEquals(42L, memory.getUserId());
        assertEquals(7L, memory.getConversationId());
        assertEquals("USER", memory.getScope());
        assertEquals("PREFERENCE", memory.getMemoryType());
        assertEquals("RESPONSE_FORMAT", memory.getSubType());
        assertEquals("AGENT", memory.getSourceType());
        assertEquals("新标题", memory.getTitle());
        assertEquals("用户偏好简洁回答。", memory.getContent());
        assertEquals("用户再次强调简洁输出。", memory.getReason());
        assertEquals(1, memory.getEnable());
        assertEquals(3, memory.getAccessCount());
        assertEquals(now, memory.getUpdatedAt());
    }
}
