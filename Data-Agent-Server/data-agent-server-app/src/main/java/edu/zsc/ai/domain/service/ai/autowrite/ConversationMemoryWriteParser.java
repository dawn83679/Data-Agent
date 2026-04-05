package edu.zsc.ai.domain.service.ai.autowrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses AI JSON response into MemoryWriteItem list.
 */
public final class ConversationMemoryWriteParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConversationMemoryWriteParser() {
    }

    public static List<MemoryWriteItem> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        // Strip markdown code fences if present
        String cleaned = json.strip();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).strip();
            }
        }

        try {
            JsonNode root = MAPPER.readTree(cleaned);
            JsonNode itemsNode = root.get("items");
            if (itemsNode == null || !itemsNode.isArray()) {
                return Collections.emptyList();
            }

            List<MemoryWriteItem> items = new ArrayList<>();
            for (JsonNode node : itemsNode) {
                items.add(new MemoryWriteItem(
                        textOrNull(node, "operation"),
                        longOrNull(node, "memoryId"),
                        textOrNull(node, "scope"),
                        textOrNull(node, "memoryType"),
                        textOrNull(node, "subType"),
                        textOrNull(node, "title"),
                        textOrNull(node, "content"),
                        textOrNull(node, "reason")
                ));
            }
            return items;
        } catch (JsonProcessingException e) {
            return null; // null signals parse failure; caller should treat as extraction error
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    private static Long longOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull() && child.isNumber()) ? child.asLong() : null;
    }
}
