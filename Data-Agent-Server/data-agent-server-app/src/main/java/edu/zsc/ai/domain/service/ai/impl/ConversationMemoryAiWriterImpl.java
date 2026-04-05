package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.autowrite.ConversationMemoryAiWriter;
import edu.zsc.ai.domain.service.ai.autowrite.ConversationMemoryWriteParser;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext.MemorySummary;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryAiWriterImpl implements ConversationMemoryAiWriter {

    private static final Set<String> VALID_OPERATIONS = Set.of("CREATE", "UPDATE", "DELETE");
    private static final Set<String> PROMPT_ROLES = Set.of("USER", "AI");
    private static final int MAX_MESSAGE_CHARS = 500;

    private final Map<String, ChatModel> chatModelsByName;

    @Override
    public List<MemoryWriteItem> extractMemoryWrites(MemoryWriteContext context) {
        if (context.isEmpty()) {
            log.info("[MemAutoWrite:AI] Context is empty, returning empty list");
            return Collections.emptyList();
        }

        String prompt = buildPrompt(context);
        log.info("[MemAutoWrite:AI] Prompt built: length={} chars", prompt.length());
        log.debug("[MemAutoWrite:AI] Prompt content:\n{}", prompt);

        ChatModel model = chatModelsByName.get(ModelEnum.QWEN3_5_PLUS.getModelName());
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .maxOutputTokens(1024)
                .build();

        try {
            log.info("[MemAutoWrite:AI] Calling model: {}", ModelEnum.QWEN3_5_PLUS.getModelName());
            ChatResponse response = model.chat(request);
            String text = response.aiMessage().text();
            log.info("[MemAutoWrite:AI] AI response received: {} chars", text.length());
            log.info("[MemAutoWrite:AI] AI raw response:\n{}", text);

            List<MemoryWriteItem> parsed = ConversationMemoryWriteParser.parse(text);
            if (parsed == null) {
                log.warn("[MemAutoWrite:AI] Failed to parse AI response as valid JSON, returning null");
                return null;
            }
            log.info("[MemAutoWrite:AI] Parsed {} items from AI response", parsed.size());

            List<MemoryWriteItem> filtered = filterItems(parsed, context);
            log.info("[MemAutoWrite:AI] After filtering: {} -> {} items (dropped {})",
                    parsed.size(), filtered.size(), parsed.size() - filtered.size());
            return filtered;
        } catch (Exception e) {
            log.error("[MemAutoWrite:AI] AI call failed", e);
            return null;
        }
    }

    private String buildPrompt(MemoryWriteContext context) {
        String template = PromptConfig.getPrompt(PromptEnum.MEMORY_AUTO_WRITE);

        String memorySummaries = buildMemorySummaries(context);
        String messages = buildMessageText(context);

        return String.format(template, memorySummaries, messages);
    }

    private String buildMemorySummaries(MemoryWriteContext context) {
        if (context.existingMemories() == null || context.existingMemories().isEmpty()) {
            return "(No existing memories)";
        }

        StringBuilder sb = new StringBuilder();
        for (MemorySummary summary : context.existingMemories()) {
            sb.append("- ").append(summary.toPromptLine()).append("\n");
        }
        if (context.memoriesTruncated()) {
            sb.append("(... and ").append(context.totalEnabledCount() - context.existingMemories().size())
                    .append(" more memories not shown)\n");
        }
        return sb.toString();
    }

    private String buildMessageText(MemoryWriteContext context) {
        StringBuilder sb = new StringBuilder();
        for (StoredChatMessage msg : context.newMessages()) {
            if (!PROMPT_ROLES.contains(msg.getRole())) {
                continue;
            }
            String text = extractText(msg);
            if (text == null || text.isBlank()) {
                continue;
            }
            if (text.length() > MAX_MESSAGE_CHARS) {
                text = text.substring(0, MAX_MESSAGE_CHARS) + "...(truncated)";
            }
            sb.append("[").append(msg.getRole()).append("]: ").append(text).append("\n\n");
        }
        return sb.toString();
    }

    private String extractText(StoredChatMessage msg) {
        try {
            ChatMessage deserialized = ChatMessageDeserializer.messageFromJson(msg.getData());
            if (deserialized instanceof dev.langchain4j.data.message.UserMessage um) {
                return um.singleText();
            } else if (deserialized instanceof AiMessage am) {
                return am.text();
            }
            return null;
        } catch (Exception e) {
            // Fallback: use raw data if deserialization fails (e.g., in tests with plain text)
            return msg.getData();
        }
    }

    private List<MemoryWriteItem> filterItems(List<MemoryWriteItem> items, MemoryWriteContext context) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> existingIds = context.existingMemories() != null
                ? context.existingMemories().stream()
                    .map(MemorySummary::memoryId)
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        return items.stream()
                .filter(this::isValidItem)
                .filter(item -> isReferencedMemoryValid(item, existingIds))
                // Dedup: keep first occurrence per operation+memoryType+subType
                .collect(Collectors.toMap(
                        this::dedupeKey,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private boolean isValidItem(MemoryWriteItem item) {
        if (item.operation() == null || !VALID_OPERATIONS.contains(item.operation())) {
            log.info("[MemAutoWrite:AI] Rejected item: invalid operation={}", item.operation());
            return false;
        }
        boolean valid = switch (item.operation()) {
            case "CREATE" -> hasText(item.content()) && hasText(item.memoryType())
                    && isValidEnums(item.memoryType(), item.subType());
            case "UPDATE" -> item.memoryId() != null;
            case "DELETE" -> item.memoryId() != null;
            default -> false;
        };
        if (!valid) {
            log.info("[MemAutoWrite:AI] Rejected item: op={}, memoryId={}, type={}/{}, hasContent={}, hasType={}",
                    item.operation(), item.memoryId(), item.memoryType(), item.subType(),
                    hasText(item.content()), hasText(item.memoryType()));
        }
        return valid;
    }

    private boolean isValidEnums(String memoryType, String subType) {
        MemoryTypeEnum type = MemoryTypeEnum.fromCode(memoryType);
        if (type == null) {
            log.info("[MemAutoWrite:AI] Rejected CREATE: unknown memoryType={}", memoryType);
            return false;
        }
        MemorySubTypeEnum sub = MemorySubTypeEnum.fromCode(subType);
        if (sub == null || !sub.belongsTo(type)) {
            log.info("[MemAutoWrite:AI] Rejected CREATE: invalid subType={} for memoryType={}", subType, memoryType);
            return false;
        }
        return true;
    }

    private boolean isReferencedMemoryValid(MemoryWriteItem item, Set<Long> existingIds) {
        if ("CREATE".equals(item.operation())) {
            return true;
        }
        boolean valid = item.memoryId() != null && existingIds.contains(item.memoryId());
        if (!valid) {
            log.info("[MemAutoWrite:AI] Rejected {}: memoryId={} not found in existing memories",
                    item.operation(), item.memoryId());
        }
        return valid;
    }

    private String dedupeKey(MemoryWriteItem item) {
        if ("UPDATE".equals(item.operation()) || "DELETE".equals(item.operation())) {
            return item.operation() + ":" + item.memoryId();
        }
        // Only PREFERENCE enforces one-per-subType; other types allow multiple same-subType CREATEs
        if ("PREFERENCE".equals(item.memoryType())) {
            return "CREATE:PREFERENCE:" + item.subType();
        }
        return "CREATE:" + item.memoryType() + ":" + item.subType() + ":" + item.title();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
