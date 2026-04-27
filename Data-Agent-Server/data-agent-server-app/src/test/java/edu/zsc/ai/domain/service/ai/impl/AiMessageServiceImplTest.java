package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.agent.memory.ChatMemoryCompressor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMessageServiceImplTest {

    @Test
    void moveSummaryToEnd_returnsSameReference_whenNoSummary() {
        List<ChatMessage> input = List.of(
                UserMessage.from("u1"),
                AiMessage.from("a1"),
                UserMessage.from("u2")
        );

        List<ChatMessage> result = AiMessageServiceImpl.moveSummaryToEnd(input);

        assertSame(input, result);
    }

    @Test
    void moveSummaryToEnd_returnsSameReference_whenSummaryAlreadyLast() {
        List<ChatMessage> input = List.of(
                UserMessage.from("u1"),
                AiMessage.from("a1"),
                summary("history")
        );

        List<ChatMessage> result = AiMessageServiceImpl.moveSummaryToEnd(input);

        assertSame(input, result);
    }

    @Test
    void moveSummaryToEnd_movesSummaryFromFrontToEnd() {
        ChatMessage summary = summary("history");
        ChatMessage u1 = UserMessage.from("u1");
        ChatMessage a1 = AiMessage.from("a1");
        ChatMessage u2 = UserMessage.from("u2");
        List<ChatMessage> input = List.of(summary, u1, a1, u2);

        List<ChatMessage> result = AiMessageServiceImpl.moveSummaryToEnd(input);

        assertEquals(4, result.size());
        assertSame(u1, result.get(0));
        assertSame(a1, result.get(1));
        assertSame(u2, result.get(2));
        assertTrue(ChatMemoryCompressor.isSummaryMessage(result.get(3)));
    }

    @Test
    void moveSummaryToEnd_handlesEmptyList() {
        List<ChatMessage> result = AiMessageServiceImpl.moveSummaryToEnd(List.of());
        assertTrue(result.isEmpty());
    }

    private static ChatMessage summary(String body) {
        return UserMessage.from("[CONVERSATION_SUMMARY]\n" + body);
    }
}
