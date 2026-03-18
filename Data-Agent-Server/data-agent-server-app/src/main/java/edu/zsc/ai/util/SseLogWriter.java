package edu.zsc.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.zsc.ai.common.constant.ChatResponseDataKey;
import edu.zsc.ai.common.enums.ai.ChatStreamEventType;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appends SSE stream blocks to per-conversation JSONL log files for inspection.
 * Log files: {@code logs/sse/conv-{conversationId}-{timestamp}.jsonl}.
 * <p>
 * Thread-safe: uses a ConcurrentHashMap to track active conversations.
 * Auto-closes: when a DONE block is received the conversation is removed from the active map.
 */
@Slf4j
public final class SseLogWriter {

    private static final Path LOG_DIR = Paths.get("logs", "sse");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Per-conversation state holder. */
    private static final ConcurrentHashMap<Long, ConversationLog> ACTIVE = new ConcurrentHashMap<>();

    private SseLogWriter() {
    }

    // ─── state per conversation ──────────────────────────────────────────

    private static class ConversationLog {
        final Path file;
        final Instant startTime;
        int toolCount;
        final ConcurrentHashMap<String, Integer> toolCounts = new ConcurrentHashMap<>();

        ConversationLog(Path file, Instant startTime) {
            this.file = file;
            this.startTime = startTime;
        }
    }

    // ─── public API ──────────────────────────────────────────────────────

    /**
     * Append a block for the given conversation.
     */
    public static void append(ChatResponseBlock block, Long conversationId) {
        try {
            ConversationLog cl = ACTIVE.computeIfAbsent(conversationId, SseLogWriter::initConversation);
            ChatStreamEventType type = resolveType(block);

            ObjectNode line = buildLine(type, conversationId, block, cl);
            writeLine(cl.file, MAPPER.writeValueAsString(line));

            // on DONE, write summary and remove from active map
            if (type == ChatStreamEventType.DONE) {
                writeSummary(cl, conversationId);
                ACTIVE.remove(conversationId);
            }
        } catch (Exception e) {
            log.warn("Failed to append SSE block to log (conv={}): {}", conversationId, e.getMessage());
        }
    }

    /**
     * Legacy overload — extracts conversationId from the block itself.
     */
    public static void append(ChatResponseBlock block) {
        append(block, block.getConversationId());
    }

    // ─── initialisation ──────────────────────────────────────────────────

    private static ConversationLog initConversation(Long conversationId) {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
            String fileTs = LocalDateTime.now().format(FILE_TS);
            Path file = LOG_DIR.resolve("conv-" + conversationId + "-" + fileTs + ".jsonl");

            Instant startTime = Instant.now();
            ConversationLog cl = new ConversationLog(file, startTime);

            // write _META header
            ObjectNode meta = MAPPER.createObjectNode();
            meta.put("ts", now());
            meta.put("type", "_META");
            meta.put("conv", conversationId);
            ObjectNode metaData = MAPPER.createObjectNode();
            metaData.put("startTime", startTime.toString());
            metaData.put("conversationId", conversationId);
            meta.set("data", metaData);

            writeLine(file, MAPPER.writeValueAsString(meta));
            return cl;
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialise SSE log for conv=" + conversationId, e);
        }
    }

    // ─── line builders ───────────────────────────────────────────────────

    private static ObjectNode buildLine(ChatStreamEventType type, Long conversationId, ChatResponseBlock block, ConversationLog cl) {
        return switch (type) {
            case TOOL_CALL -> buildToolCallLine(conversationId, block, cl);
            case TOOL_RESULT -> buildToolResultLine(conversationId, block, cl);
            case STATUS -> buildStatusLine(conversationId, block);
            case SUB_AGENT_START, SUB_AGENT_PROGRESS, SUB_AGENT_COMPLETE, SUB_AGENT_ERROR ->
                    buildSubAgentLine(type, conversationId, block);
            default -> buildGenericLine(type, conversationId, block);
        };
    }

    private static ObjectNode buildToolCallLine(Long conversationId, ChatResponseBlock block, ConversationLog cl) {
        ObjectNode line = baseNode(ChatStreamEventType.TOOL_CALL, conversationId);
        JsonNode parsed = parseData(block.getData());
        if (parsed != null && parsed.isObject()) {
            copyIfPresent(line, parsed, ChatResponseBlock.DATA_KEY_ID, "toolCallId");
            String toolName = textOrNull(parsed, ChatResponseBlock.DATA_KEY_TOOL_NAME);
            line.put("toolName", toolName != null ? toolName : "");

            // parse arguments as JSON object if possible, else as string
            JsonNode argsNode = parsed.get(ChatResponseBlock.DATA_KEY_ARGUMENTS);
            if (argsNode != null) {
                if (argsNode.isObject()) {
                    line.set("args", argsNode);
                } else {
                    String argsText = argsNode.asText("");
                    JsonNode argsParsed = parseData(argsText);
                    if (argsParsed != null && argsParsed.isObject()) {
                        line.set("args", argsParsed);
                    } else {
                        line.put("args", argsText);
                    }
                }
            }

            copyBooleanIfPresent(line, parsed, ChatResponseBlock.DATA_KEY_STREAMING, "streaming");
            if (block.getParentToolCallId() != null) {
                line.put("parentToolCallId", block.getParentToolCallId());
            }

            // track tool counts (only for non-streaming or streaming=false, to avoid double-counting)
            Boolean streaming = booleanOrNull(parsed, ChatResponseBlock.DATA_KEY_STREAMING);
            if (streaming == null || !streaming) {
                cl.toolCount++;
                if (toolName != null && !toolName.isEmpty()) {
                    cl.toolCounts.merge(toolName, 1, Integer::sum);
                }
            }
        } else {
            line.put("data", block.getData() != null ? block.getData() : "");
        }
        return line;
    }

    private static ObjectNode buildToolResultLine(Long conversationId, ChatResponseBlock block, ConversationLog cl) {
        ObjectNode line = baseNode(ChatStreamEventType.TOOL_RESULT, conversationId);
        JsonNode parsed = parseData(block.getData());
        if (parsed != null && parsed.isObject()) {
            copyIfPresent(line, parsed, ChatResponseBlock.DATA_KEY_ID, "toolCallId");
            String toolName = textOrNull(parsed, ChatResponseBlock.DATA_KEY_TOOL_NAME);
            line.put("toolName", toolName != null ? toolName : "");

            // result length instead of full result
            String result = textOrNull(parsed, ChatResponseBlock.DATA_KEY_RESULT);
            line.put("resultLen", result != null ? result.length() : 0);

            copyBooleanIfPresent(line, parsed, ChatResponseBlock.DATA_KEY_ERROR, "error");
            if (block.getParentToolCallId() != null) {
                line.put("parentToolCallId", block.getParentToolCallId());
            }
        } else {
            line.put("data", block.getData() != null ? block.getData() : "");
        }
        return line;
    }

    private static ObjectNode buildStatusLine(Long conversationId, ChatResponseBlock block) {
        ObjectNode line = baseNode(ChatStreamEventType.STATUS, conversationId);
        JsonNode parsed = parseData(block.getData());
        if (parsed != null && parsed.isObject()) {
            line.set("data", parsed);
        } else {
            line.put("data", block.getData() != null ? block.getData() : "");
        }
        return line;
    }

    private static ObjectNode buildSubAgentLine(ChatStreamEventType type, Long conversationId, ChatResponseBlock block) {
        ObjectNode line = baseNode(type, conversationId);
        JsonNode parsed = parseData(block.getData());
        if (parsed != null && parsed.isObject()) {
            line.set("data", parsed);
        } else {
            line.put("data", block.getData() != null ? block.getData() : "");
        }
        if (block.getParentToolCallId() != null) {
            line.put("parentToolCallId", block.getParentToolCallId());
        }
        if (block.getSubAgentTaskId() != null) {
            line.put("subAgentTaskId", block.getSubAgentTaskId());
        }
        return line;
    }

    private static ObjectNode buildGenericLine(ChatStreamEventType type, Long conversationId, ChatResponseBlock block) {
        ObjectNode line = baseNode(type, conversationId);
        line.put("data", block.getData() != null ? block.getData() : "");
        return line;
    }

    // ─── summary on DONE ─────────────────────────────────────────────────

    private static void writeSummary(ConversationLog cl, Long conversationId) {
        try {
            long durationMs = Instant.now().toEpochMilli() - cl.startTime.toEpochMilli();

            ObjectNode line = baseNode(ChatStreamEventType.DONE, conversationId);
            ObjectNode data = MAPPER.createObjectNode();
            data.put(ChatResponseDataKey.TOOL_COUNT, cl.toolCount);
            data.set(ChatResponseDataKey.TOOL_COUNTS, MAPPER.valueToTree(cl.toolCounts));

            // try to extract token info from the DONE block data if available
            // (already written as the main DONE line; this is the summary)
            data.put("durationMs", durationMs);
            line.set("data", data);

            writeLine(cl.file, MAPPER.writeValueAsString(line));
        } catch (Exception e) {
            log.warn("Failed to write SSE summary (conv={}): {}", conversationId, e.getMessage());
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private static ObjectNode baseNode(ChatStreamEventType type, Long conversationId) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("ts", now());
        node.put("type", type.name());
        node.put("conv", conversationId);
        return node;
    }

    private static ChatStreamEventType resolveType(ChatResponseBlock block) {
        return ChatStreamEventType.resolve(block);
    }

    private static String now() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    private static JsonNode parseData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(data);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asText(null);
    }

    private static Boolean booleanOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asBoolean();
    }

    private static void copyIfPresent(ObjectNode target, JsonNode source, String sourceField, String targetField) {
        JsonNode child = source.get(sourceField);
        if (child != null && !child.isNull()) {
            target.put(targetField, child.asText());
        }
    }

    private static void copyBooleanIfPresent(ObjectNode target, JsonNode source, String sourceField, String targetField) {
        JsonNode child = source.get(sourceField);
        if (child != null && !child.isNull()) {
            target.put(targetField, child.asBoolean());
        }
    }

    private static void writeLine(Path file, String line) throws IOException {
        Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
