package edu.zsc.ai.domain.service.ai.autowrite;

import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationMemoryWriteParserTest {

    @Test
    void parse_acceptsDirectJsonPayload() {
        String json = """
                {
                  "items": [
                    {
                      "operation": "UPDATE",
                      "memoryId": 12,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "LANGUAGE_PREFERENCE",
                      "title": "默认回复语言",
                      "content": "默认使用中文回答",
                      "reason": "用户在最新对话中明确改了偏好"
                    }
                  ]
                }
                """;

        List<MemoryWriteItem> parsed = ConversationMemoryWriteParser.parse(json);

        assertEquals(1, parsed.size());
        assertEquals("UPDATE", parsed.get(0).operation());
        assertEquals(12L, parsed.get(0).memoryId());
        assertEquals("默认使用中文回答", parsed.get(0).content());
    }

    @Test
    void parse_acceptsMarkdownFencedJsonPayload() {
        String json = """
                ```json
                {
                  "items": [
                    {
                      "operation": "DELETE",
                      "memoryId": 18
                    }
                  ]
                }
                ```
                """;

        List<MemoryWriteItem> parsed = ConversationMemoryWriteParser.parse(json);

        assertEquals(1, parsed.size());
        assertEquals("DELETE", parsed.get(0).operation());
        assertEquals(18L, parsed.get(0).memoryId());
    }

    @Test
    void parse_returnsEmptyListForExplicitEmptyItems() {
        assertTrue(ConversationMemoryWriteParser.parse("{\"items\": []}").isEmpty());
    }

    @Test
    void parse_returnsEmptyListWhenItemsFieldIsMissingOrInvalid() {
        assertIterableEquals(List.of(), ConversationMemoryWriteParser.parse("{\"foo\":\"bar\"}"));
        assertIterableEquals(List.of(), ConversationMemoryWriteParser.parse("{\"items\":{}}"));
    }

    @Test
    void parse_returnsEmptyListForBlankInput() {
        assertTrue(ConversationMemoryWriteParser.parse("   ").isEmpty());
        assertTrue(ConversationMemoryWriteParser.parse(null).isEmpty());
    }

    @Test
    void parse_returnsNullForMalformedJson() {
        assertNull(ConversationMemoryWriteParser.parse("{not-json}"));
    }
}
