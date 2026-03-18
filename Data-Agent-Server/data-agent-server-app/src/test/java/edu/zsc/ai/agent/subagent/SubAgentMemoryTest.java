package edu.zsc.ai.agent.subagent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubAgentMemoryTest {

    @Test
    void createTemporaryMemory_isIndependent() {
        ChatMemory memory1 = SubAgentMemoryFactory.createTemporary();
        ChatMemory memory2 = SubAgentMemoryFactory.createTemporary();

        memory1.add(UserMessage.from("hello from memory1"));

        // memory2 should be completely independent
        assertEquals(1, memory1.messages().size());
        assertEquals(0, memory2.messages().size());
    }

    @Test
    void createTemporaryMemory_keepsAllMessages() {
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();

        for (int i = 0; i < 240; i++) {
            memory.add(UserMessage.from("msg" + i));
        }

        assertEquals(240, memory.messages().size());
        assertEquals("msg0", ((UserMessage) memory.messages().get(0)).singleText());
        assertEquals("msg239", ((UserMessage) memory.messages().get(239)).singleText());
    }

    @Test
    void buildContext_instructionOnly() {
        String result = SubAgentPromptBuilder.builder()
                .instruction("show me all orders")
                .connectionId(1L)
                .build();

        assertTrue(result.contains("show me all orders"));
        assertTrue(result.contains("1"));
    }

    @Test
    void buildContext_withContext() {
        String result = SubAgentPromptBuilder.builder()
                .instruction("filter by last month")
                .connectionId(1L)
                .context("User was exploring the orders table. Previous query returned 500 rows.")
                .build();

        assertTrue(result.contains("filter by last month"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("orders table"));
    }

    @Test
    void buildContext_withErrorInContext() {
        String result = SubAgentPromptBuilder.builder()
                .instruction("show me all orders")
                .connectionId(1L)
                .context("Previous error: column \"amount\" does not exist")
                .build();

        assertTrue(result.contains("show me all orders"));
        assertTrue(result.contains("amount"));
        assertTrue(result.contains("does not exist"));
    }
}
