package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomChatMemoryStoreTest {

    @Test
    void moveCompactionContextToFront_returnsSameReference_whenNoCompactionContext() {
        List<ChatMessage> input = List.of(
                UserMessage.from("u1"),
                AiMessage.from("a1"),
                UserMessage.from("[CONVERSATION_SUMMARY]\nlegacy is normal now")
        );

        List<ChatMessage> result = CustomChatMemoryStore.moveCompactionContextToFront(input);

        assertSame(input, result);
    }

    @Test
    void moveCompactionContextToFront_returnsSameReference_whenCompactionContextAlreadyFirst() {
        List<ChatMessage> input = List.of(
                compactionContext("old history"),
                UserMessage.from("u1"),
                AiMessage.from("a1")
        );

        List<ChatMessage> result = CustomChatMemoryStore.moveCompactionContextToFront(input);

        assertSame(input, result);
    }

    @Test
    void moveCompactionContextToFront_movesCompactionContextFromEndToFront() {
        ChatMessage summary = compactionContext("history");
        ChatMessage u1 = UserMessage.from("u1");
        ChatMessage a1 = AiMessage.from("a1");
        ChatMessage u2 = UserMessage.from("u2");
        List<ChatMessage> input = List.of(u1, a1, u2, summary);

        List<ChatMessage> result = CustomChatMemoryStore.moveCompactionContextToFront(input);

        assertEquals(4, result.size());
        assertTrue(ChatMemoryCompressor.isCompactionContextMessage(result.get(0)));
        assertSame(u1, result.get(1));
        assertSame(a1, result.get(2));
        assertSame(u2, result.get(3));
    }

    @Test
    void moveCompactionContextToFront_handlesEmptyList() {
        List<ChatMessage> result = CustomChatMemoryStore.moveCompactionContextToFront(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPersistRuntimeMessage_filtersPlainSystemButKeepsCompactionSystem() {
        assertTrue(CustomChatMemoryStore.shouldPersistRuntimeMessage(UserMessage.from("u1")));
        assertTrue(CustomChatMemoryStore.shouldPersistRuntimeMessage(compactionContext("history")));
        assertEquals(false, CustomChatMemoryStore.shouldPersistRuntimeMessage(SystemMessage.from("runtime prompt")));
    }

    private static ChatMessage compactionContext(String body) {
        return SystemMessage.from(CompactionContextSupport.buildContinuationMessage(body, true, true));
    }
}
