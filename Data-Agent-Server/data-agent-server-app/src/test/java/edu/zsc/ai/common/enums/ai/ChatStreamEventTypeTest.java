package edu.zsc.ai.common.enums.ai;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatStreamEventTypeTest {

    @Test
    void resolvesFromBlockType() {
        ChatResponseBlock block = ChatResponseBlock.text("hello");
        assertEquals(ChatStreamEventType.TEXT, ChatStreamEventType.resolve(block));
    }

    @Test
    void resolvesDoneWhenBlockHasNoType() {
        ChatResponseBlock block = ChatResponseBlock.doneBlock();
        assertEquals(ChatStreamEventType.DONE, ChatStreamEventType.resolve(block));
    }

    @Test
    void resolvesUnknownForUnexpectedType() {
        ChatResponseBlock block = ChatResponseBlock.builder().type("WHATEVER").done(false).build();
        assertEquals(ChatStreamEventType.UNKNOWN, ChatStreamEventType.resolve(block));
    }
}
