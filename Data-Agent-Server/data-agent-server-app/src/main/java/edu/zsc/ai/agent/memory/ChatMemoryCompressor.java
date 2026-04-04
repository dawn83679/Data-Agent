package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.constant.CompressionLogConstant;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.domain.event.MemoryCompressionStartedEvent;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.CompressionService;
import edu.zsc.ai.domain.service.ai.model.CompressionDoneMetadata;
import edu.zsc.ai.domain.service.ai.model.CompressionResult;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryCompressor {

    static final String SUMMARY_PREFIX = "[CONVERSATION_SUMMARY]\n";

    private static final double COMPRESSION_RATIO = 0.75;
    private static final int MIN_MESSAGES_FOR_COMPRESSION = 4;

    private final AiConversationService aiConversationService;
    private final AiMessageService aiMessageService;
    private final CompressionService compressionService;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentLogService agentLogService;
    private final AiModelCatalog aiModelCatalog;

    private final Set<Long> compressingConversations = ConcurrentHashMap.newKeySet();
    private final Map<Long, Map<String, Object>> completedCompressionMetadata = new ConcurrentHashMap<>();

    /**
     * Compresses the given messages if the conversation's accumulated token count
     * exceeds the model's memory threshold.
     *
     * @return compressed message list, or the original list if compression was not needed / failed
     */
    public List<ChatMessage> compressIfNeeded(Long conversationId, String modelName, List<ChatMessage> messages) {
        if (modelName == null || messages.size() < MIN_MESSAGES_FOR_COMPRESSION) {
            return messages;
        }

        CompressionCheck check = getCompressionCheck(conversationId, modelName);
        if (!check.exceeded()) {
            return messages;
        }

        if (!compressingConversations.add(conversationId)) {
            recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_SKIPPED, AgentLogFields.of(
                    CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_SKIP_ALREADY_IN_PROGRESS,
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, check.tokenCount(),
                    CompressionLogConstant.FIELD_THRESHOLD, check.threshold(),
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            return messages;
        }

        try {
            recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_STARTED, AgentLogFields.of(
                    CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_COMPRESS,
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, check.tokenCount(),
                    CompressionLogConstant.FIELD_THRESHOLD, check.threshold(),
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            eventPublisher.publishEvent(new MemoryCompressionStartedEvent(this, conversationId));
            return doCompress(conversationId, modelName, check.tokenCount(), check.threshold(), messages, true, true).messages();
        } catch (Exception e) {
            recordCompressionError(conversationId, CompressionLogConstant.EVENT_COMPRESSION_FAILED, e, AgentLogFields.of(
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, check.tokenCount(),
                    CompressionLogConstant.FIELD_THRESHOLD, check.threshold(),
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            log.warn("Compression failed for conversation {}, keeping original messages", conversationId, e);
            return messages;
        } finally {
            compressingConversations.remove(conversationId);
        }
    }

    public Map<String, Object> consumeDoneMetadata(Long conversationId) {
        if (conversationId == null) {
            return Map.of();
        }
        Map<String, Object> metadata = completedCompressionMetadata.remove(conversationId);
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(metadata);
    }

    public CompressionDoneMetadata compressNow(Long conversationId, String modelName) {
        if (conversationId == null || modelName == null) {
            return new CompressionDoneMetadata(false, null, null, 0, 0, null, null, null);
        }

        AiConversation conversation = aiConversationService.getByIdForCurrentUser(conversationId);
        Integer tokenCountBefore = conversation == null ? null : conversation.getTokenCount();
        int threshold = resolveMemoryThreshold(modelName);
        List<StoredChatMessage> storedMessages = aiMessageService.getActiveByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessage> messages = storedMessages.stream()
                .map(item -> ChatMessageDeserializer.messageFromJson(item.getData()))
                .toList();

        if (messages.size() < MIN_MESSAGES_FOR_COMPRESSION) {
            recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_SKIPPED, AgentLogFields.of(
                    CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_SKIP_NOT_ENOUGH_MESSAGES,
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, tokenCountBefore,
                    CompressionLogConstant.FIELD_THRESHOLD, threshold,
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            return new CompressionDoneMetadata(false, tokenCountBefore, tokenCountBefore, 0, messages.size(), null, null, null);
        }

        if (!compressingConversations.add(conversationId)) {
            recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_SKIPPED, AgentLogFields.of(
                    CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_SKIP_ALREADY_IN_PROGRESS,
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, tokenCountBefore,
                    CompressionLogConstant.FIELD_THRESHOLD, threshold,
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            return new CompressionDoneMetadata(false, tokenCountBefore, tokenCountBefore, 0, messages.size(), null, null, null);
        }

        try {
            recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_STARTED, AgentLogFields.of(
                    CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_COMPRESS,
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, tokenCountBefore,
                    CompressionLogConstant.FIELD_THRESHOLD, threshold,
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            ManualCompressionResult result = doCompress(
                    conversationId,
                    modelName,
                    tokenCountBefore,
                    threshold,
                    messages,
                    true,
                    false
            );
            aiMessageService.replaceConversationMessages(conversationId, result.messages());
            return new CompressionDoneMetadata(
                    true,
                    result.tokenCountBefore(),
                    result.tokenCountAfter(),
                    result.compressedMessageCount(),
                    result.keptRecentCount(),
                    result.summary(),
                    result.compressionOutputTokens(),
                    result.compressionTotalTokens()
            );
        } catch (Exception e) {
            recordCompressionError(conversationId, CompressionLogConstant.EVENT_COMPRESSION_FAILED, e, AgentLogFields.of(
                    CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                    CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, tokenCountBefore,
                    CompressionLogConstant.FIELD_THRESHOLD, threshold,
                    CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size()
            ));
            log.warn("Manual compression failed for conversation {}, keeping original messages", conversationId, e);
            return new CompressionDoneMetadata(false, tokenCountBefore, tokenCountBefore, 0, messages.size(), null, null, null);
        } finally {
            compressingConversations.remove(conversationId);
        }
    }

    public static boolean isSummaryMessage(ChatMessage message) {
        return message instanceof UserMessage um && um.singleText().startsWith(SUMMARY_PREFIX);
    }

    private CompressionCheck getCompressionCheck(Long conversationId, String modelName) {
        AiConversation conversation = aiConversationService.getById(conversationId);
        if (conversation == null || conversation.getTokenCount() == null) {
            return CompressionCheck.notExceeded(null, resolveMemoryThreshold(modelName));
        }

        int threshold = resolveMemoryThreshold(modelName);
        Integer tokenCount = conversation.getTokenCount();
        return new CompressionCheck(tokenCount, threshold, tokenCount >= threshold);
    }

    private ManualCompressionResult doCompress(Long conversationId,
                                               String modelName,
                                               Integer tokenCountBefore,
                                               int threshold,
                                               List<ChatMessage> messages,
                                               boolean updateConversationTokenCount,
                                               boolean shouldRememberDoneMetadata) {
        int splitIndex = findCleanSplitPoint(messages,
                (int) Math.ceil(messages.size() * COMPRESSION_RATIO));

        List<ChatMessage> toCompress = messages.subList(0, splitIndex);
        List<ChatMessage> toKeep = messages.subList(splitIndex, messages.size());

        CompressionResult compressionResult = compressionService.compress(toCompress);
        String summary = compressionResult.summary();
        Integer tokenCountAfter = updateConversationTokenCount
                ? normalizePositive(compressionResult.outputTokens())
                : null;
        if (updateConversationTokenCount && tokenCountAfter != null) {
            aiConversationService.updateTokenCount(conversationId, tokenCountAfter);
        }

        if (shouldRememberDoneMetadata) {
            rememberDoneMetadata(conversationId, tokenCountBefore, tokenCountAfter, compressionResult, toCompress.size(), toKeep.size());
        }
        recordCompressionEvent(conversationId, CompressionLogConstant.EVENT_COMPRESSION_COMPLETED, AgentLogFields.of(
                CompressionLogConstant.FIELD_DECISION, CompressionLogConstant.DECISION_COMPRESSED,
                CompressionLogConstant.FIELD_MODEL_NAME, modelName,
                CompressionLogConstant.FIELD_TOKEN_COUNT_BEFORE, tokenCountBefore,
                CompressionLogConstant.FIELD_TOKEN_COUNT_AFTER, tokenCountAfter,
                CompressionLogConstant.FIELD_THRESHOLD, threshold,
                CompressionLogConstant.FIELD_MESSAGE_COUNT, messages.size(),
                CompressionLogConstant.FIELD_COMPRESSED_MESSAGE_COUNT, toCompress.size(),
                CompressionLogConstant.FIELD_KEPT_RECENT_COUNT, toKeep.size(),
                CompressionLogConstant.FIELD_SUMMARY_LENGTH, summary.length(),
                CompressionLogConstant.FIELD_OUTPUT_TOKENS, compressionResult.outputTokens(),
                CompressionLogConstant.FIELD_TOTAL_TOKENS, compressionResult.totalTokens()
        ));
        log.info("Conversation {} compressed: tokenCount {} -> {}, threshold={}, compressedMessages={}, keptRecent={}, summaryChars={}, outputTokens={}, totalTokens={}",
                conversationId,
                tokenCountBefore,
                tokenCountAfter,
                threshold,
                toCompress.size(),
                toKeep.size(),
                summary.length(),
                compressionResult.outputTokens(),
                compressionResult.totalTokens());

        List<ChatMessage> compressedMessages = new ArrayList<>(toKeep.size() + 1);
        compressedMessages.addAll(toKeep);
        compressedMessages.add(UserMessage.from(SUMMARY_PREFIX + summary));
        return new ManualCompressionResult(
                compressedMessages,
                tokenCountBefore,
                tokenCountAfter,
                toCompress.size(),
                toKeep.size(),
                summary,
                compressionResult.outputTokens(),
                compressionResult.totalTokens()
        );
    }

    /**
     * Scans backward from targetIndex to find a clean split point — a UserMessage
     * or a standalone AiMessage (no tool execution requests) — to avoid splitting
     * a tool-call / tool-result pair.
     */
    private int findCleanSplitPoint(List<ChatMessage> messages, int targetIndex) {
        for (int i = targetIndex; i > 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.type() == ChatMessageType.USER) {
                return i;
            }
            if (msg.type() == ChatMessageType.AI && !((AiMessage) msg).hasToolExecutionRequests()) {
                return i;
            }
        }
        return targetIndex;
    }

    private int resolveMemoryThreshold(String modelName) {
        if (!aiModelCatalog.supports(modelName)) {
            log.warn("Unknown model '{}', falling back to default threshold", modelName);
        }
        int threshold = aiModelCatalog.resolveMemoryThreshold(modelName);
        return threshold;
    }

    private void rememberDoneMetadata(Long conversationId,
                                      Integer tokenCountBefore,
                                      Integer tokenCountAfter,
                                      CompressionResult result,
                                      int compressedMessageCount,
                                      int keptRecentCount) {
        CompressionDoneMetadata metadata = new CompressionDoneMetadata(
                true,
                tokenCountBefore,
                tokenCountAfter,
                compressedMessageCount,
                keptRecentCount,
                result.summary(),
                result.outputTokens(),
                result.totalTokens()
        );
        completedCompressionMetadata.put(conversationId, metadata.toMap());
    }

    private void recordCompressionEvent(Long conversationId, String eventName, Map<String, Object> payload) {
        Map<String, Object> data = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        data.put("eventName", eventName);
        agentLogService.record(AgentLogEvent.builder()
                .type(AgentLogType.DEBUG_EVENT)
                .loggerName(CompressionLogConstant.LOGGER_NAME)
                .conversationId(conversationId)
                .message(eventName)
                .payload(data)
                .build());
    }

    private void recordCompressionError(Long conversationId, String eventName, Throwable throwable, Map<String, Object> payload) {
        Map<String, Object> data = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        data.put("eventName", eventName);
        agentLogService.recordError(AgentLogType.DEBUG_EVENT, CompressionLogConstant.LOGGER_NAME, eventName, throwable, data);
    }

    private Integer normalizePositive(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private record ManualCompressionResult(
            List<ChatMessage> messages,
            Integer tokenCountBefore,
            Integer tokenCountAfter,
            int compressedMessageCount,
            int keptRecentCount,
            String summary,
            Integer compressionOutputTokens,
            Integer compressionTotalTokens
    ) {
    }

    private record CompressionCheck(Integer tokenCount, int threshold, boolean exceeded) {
        private static CompressionCheck notExceeded(Integer tokenCount, int threshold) {
            return new CompressionCheck(tokenCount, threshold, false);
        }
    }
}
