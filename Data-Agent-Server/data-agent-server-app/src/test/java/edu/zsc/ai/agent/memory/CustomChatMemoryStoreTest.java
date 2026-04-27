package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomChatMemoryStoreTest {

    @Test
    void moveSummaryToFront_returnsSameReference_whenNoSummary() {
        List<ChatMessage> input = List.of(
                UserMessage.from("u1"),
                AiMessage.from("a1"),
                UserMessage.from("u2")
        );

        List<ChatMessage> result = CustomChatMemoryStore.moveSummaryToFront(input);

        assertSame(input, result);
    }

    @Test
    void moveSummaryToFront_returnsSameReference_whenSummaryAlreadyFirst() {
        List<ChatMessage> input = List.of(
                summary("old history"),
                UserMessage.from("u1"),
                AiMessage.from("a1")
        );

        List<ChatMessage> result = CustomChatMemoryStore.moveSummaryToFront(input);

        assertSame(input, result);
    }

    @Test
    void moveSummaryToFront_movesSummaryFromEndToFront() {
        ChatMessage summary = summary("history");
        ChatMessage u1 = UserMessage.from("u1");
        ChatMessage a1 = AiMessage.from("a1");
        ChatMessage u2 = UserMessage.from("u2");
        List<ChatMessage> input = List.of(u1, a1, u2, summary);

        List<ChatMessage> result = CustomChatMemoryStore.moveSummaryToFront(input);

        assertEquals(4, result.size());
        assertTrue(ChatMemoryCompressor.isSummaryMessage(result.get(0)));
        assertSame(u1, result.get(1));
        assertSame(a1, result.get(2));
        assertSame(u2, result.get(3));
    }

    @Test
    void moveSummaryToFront_handlesEmptyList() {
        List<ChatMessage> result = CustomChatMemoryStore.moveSummaryToFront(List.of());
        assertTrue(result.isEmpty());
    }

    private static ChatMessage summary(String body) {
        return UserMessage.from("[CONVERSATION_SUMMARY]\n" + body);
    }
}
