package edu.zsc.ai.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationLogIteratorTest {

    @TempDir
    Path tempDir;

    @Test
    void iterator_skipsMalformedLinesAndYieldsStructuredEvents() throws IOException {
        Path file = tempDir.resolve("20260318120000-42.log");
        AgentLogEvent event = AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(AgentLogType.CONVERSATION_START)
                .conversationId(42L)
                .message("conversation_start")
                .build();
        Files.write(
                file,
                List.of("not-json", edu.zsc.ai.util.JsonUtil.object2json(event)),
                StandardCharsets.UTF_8);

        try (ConversationLogIterator iterator = new ConversationLogIterator(file)) {
            assertTrue(iterator.hasNext());
            AgentLogEvent actual = iterator.next();
            assertEquals(AgentLogType.CONVERSATION_START, actual.getType());
            assertEquals(42L, actual.getConversationId());
            assertEquals("conversation_start", actual.getMessage());
        }
    }
}
