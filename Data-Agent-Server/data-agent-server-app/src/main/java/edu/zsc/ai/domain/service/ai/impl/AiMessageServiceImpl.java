package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import edu.zsc.ai.agent.memory.ChatMemoryCompressor;
import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.common.enums.ai.MessageStatusEnum;
import edu.zsc.ai.domain.mapper.ai.AiMessageMapper;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, StoredChatMessage>
        implements AiMessageService {

    @Override
    public List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .orderByAsc(StoredChatMessage::getCreatedAt)
                .orderByAsc(StoredChatMessage::getId);
        return list(wrapper);
    }

    @Override
    public List<StoredChatMessage> getActiveByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .ne(StoredChatMessage::getStatus, MessageStatusEnum.COMPRESSED.getCode())
                .ne(StoredChatMessage::getStatus, MessageStatusEnum.DELETED.getCode())
                .orderByAsc(StoredChatMessage::getCreatedAt)
                .orderByAsc(StoredChatMessage::getId);
        return list(wrapper);
    }

    @Override
<<<<<<< HEAD
    public List<StoredChatMessage> getActiveMessagesForAutoWrite(Long conversationId) {
        return getActiveByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public List<StoredChatMessage> getMessagesForAutoWriteAfter(Long conversationId, Long lastProcessedMessageId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .ne(StoredChatMessage::getStatus, MessageStatusEnum.COMPRESSED.getCode())
                .ne(StoredChatMessage::getStatus, MessageStatusEnum.DELETED.getCode())
                .gt(StoredChatMessage::getId, lastProcessedMessageId)
                .orderByAsc(StoredChatMessage::getCreatedAt)
                .orderByAsc(StoredChatMessage::getId);
        return list(wrapper);
    }

    @Override
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    public void saveBatchMessages(List<StoredChatMessage> messages) {
        saveBatch(messages);
    }

    @Override
    @Transactional
    public int removeByConversationId(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId);
        int count = (int) count(wrapper);
        if (count == 0) {
            return 0;
        }
        remove(wrapper);
        log.debug("Deleted {} messages for conversation {}", count, conversationId);
        return count;
    }

    @Override
    @Transactional
    public void replaceConversationMessages(Long conversationId, List<ChatMessage> messages) {
        List<StoredChatMessage> storedAll = getByConversationIdOrderByCreatedAtAsc(conversationId);
        List<StoredChatMessage> activeStored = storedAll.stream()
                .filter(message -> !isCompressed(message) && !isDeleted(message))
                .toList();
        List<ChatMessage> normalizedIncoming = messages.stream()
                .map(MemoryUtil::normalizeUserMessage)
                .toList();

        if (activeStored.isEmpty()) {
            appendMessages(conversationId, normalizedIncoming, nextBaseTime(storedAll));
            return;
        }

        List<ChatMessage> activeExisting = activeStored.stream()
                .map(this::deserializeMessage)
                .filter(Objects::nonNull)
                .toList();

        if (isCompressionSync(normalizedIncoming)) {
            if (syncCompressionResult(conversationId, storedAll, activeStored, activeExisting, normalizedIncoming)) {
                return;
            }
            log.warn("Compression sync fallback for conversation {}, rebuilding active messages", conversationId);
        } else {
            int commonPrefix = commonPrefixLength(activeExisting, normalizedIncoming);
            if (commonPrefix == activeExisting.size()) {
                appendMessages(conversationId, normalizedIncoming.subList(commonPrefix, normalizedIncoming.size()), nextBaseTime(storedAll));
                return;
            }
            log.warn("Active message divergence for conversation {}, rebuilding active messages", conversationId);
        }

        rebuildActiveMessages(conversationId, storedAll, activeStored, normalizedIncoming);
    }

    @Override
    public void updateLastAiMessageTokenCount(Long conversationId, Integer tokenCount) {
        if (conversationId == null || tokenCount == null) {
            return;
        }

        // Find the last AI message
        LambdaQueryWrapper<StoredChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .eq(StoredChatMessage::getRole, ChatMessageType.AI.name())
                .orderByDesc(StoredChatMessage::getCreatedAt)
                .orderByDesc(StoredChatMessage::getId)
                .last("LIMIT 1");

        StoredChatMessage lastAiMessage = getOne(queryWrapper);
        if (lastAiMessage == null) {
            log.warn("No AI message found for conversation {}", conversationId);
            return;
        }

        // Update token count
        LambdaUpdateWrapper<StoredChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StoredChatMessage::getId, lastAiMessage.getId())
                .set(StoredChatMessage::getTokenCount, tokenCount)
                .set(StoredChatMessage::getUpdatedAt, java.time.LocalDateTime.now());

        boolean updated = update(updateWrapper);
        if (updated) {
            log.info("Updated token count for message {} in conversation {}: {} tokens",
                    lastAiMessage.getId(), conversationId, tokenCount);
        }
    }

    private boolean syncCompressionResult(Long conversationId,
                                          List<StoredChatMessage> storedAll,
                                          List<StoredChatMessage> activeStored,
                                          List<ChatMessage> activeExisting,
                                          List<ChatMessage> normalizedIncoming) {
        if (normalizedIncoming.isEmpty()) {
            return false;
        }

        List<ChatMessage> keptMessages = normalizedIncoming.subList(0, normalizedIncoming.size() - 1);
        int suffixStart = findSuffixStart(activeExisting, keptMessages);
        if (suffixStart < 0) {
            return false;
        }

        markCompressed(activeStored.subList(0, suffixStart));
        appendMessages(conversationId, normalizedIncoming.subList(normalizedIncoming.size() - 1, normalizedIncoming.size()), nextBaseTime(storedAll));
        return true;
    }

    private void rebuildActiveMessages(Long conversationId,
                                       List<StoredChatMessage> storedAll,
                                       List<StoredChatMessage> activeStored,
                                       List<ChatMessage> normalizedIncoming) {
        if (CollectionUtils.isNotEmpty(activeStored)) {
            removeByIds(activeStored.stream().map(StoredChatMessage::getId).filter(Objects::nonNull).toList());
        }
        appendMessages(conversationId, normalizedIncoming, nextBaseTime(storedAll));
    }

    private void appendMessages(Long conversationId, List<ChatMessage> messages, LocalDateTime baseTime) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        List<StoredChatMessage> toSave = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage normalized = messages.get(i);
            toSave.add(StoredChatMessage.builder()
                    .conversationId(conversationId)
                    .role(normalized.type().name())
                    .tokenCount(0)
                    .status(resolveStatus(normalized))
                    .data(ChatMessageSerializer.messageToJson(normalized))
                    .createdAt(baseTime.plusNanos(i * 1000L))
                    .updatedAt(baseTime)
                    .build());
        }
        saveBatchMessages(toSave);
    }

    private void markCompressed(List<StoredChatMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        List<Long> ids = messages.stream()
                .map(StoredChatMessage::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<StoredChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(StoredChatMessage::getId, ids)
                .set(StoredChatMessage::getStatus, MessageStatusEnum.COMPRESSED.getCode())
                .set(StoredChatMessage::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
    }

    private LocalDateTime nextBaseTime(List<StoredChatMessage> storedAll) {
        return storedAll.stream()
                .map(StoredChatMessage::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now())
                .plusNanos(1000L);
    }

    private boolean isCompressionSync(List<ChatMessage> normalizedIncoming) {
        return !normalizedIncoming.isEmpty()
                && ChatMemoryCompressor.isSummaryMessage(normalizedIncoming.get(normalizedIncoming.size() - 1));
    }

    private int resolveStatus(ChatMessage message) {
        return ChatMemoryCompressor.isSummaryMessage(message)
                ? MessageStatusEnum.COMPRESSION_SUMMARY.getCode()
                : MessageStatusEnum.NORMAL.getCode();
    }

    private boolean isCompressed(StoredChatMessage message) {
        return Objects.equals(message.getStatus(), MessageStatusEnum.COMPRESSED.getCode());
    }

    private boolean isDeleted(StoredChatMessage message) {
        return Objects.equals(message.getStatus(), MessageStatusEnum.DELETED.getCode());
    }

    private ChatMessage deserializeMessage(StoredChatMessage storedMessage) {
        try {
            return MemoryUtil.normalizeUserMessage(ChatMessageDeserializer.messageFromJson(storedMessage.getData()));
        } catch (Exception e) {
            log.warn("Failed to deserialize stored message {}, skipping", storedMessage.getId(), e);
            return null;
        }
    }

    private int commonPrefixLength(List<ChatMessage> existing, List<ChatMessage> incoming) {
        int max = Math.min(existing.size(), incoming.size());
        int index = 0;
        while (index < max && messagesEqual(existing.get(index), incoming.get(index))) {
            index++;
        }
        return index;
    }

    private int findSuffixStart(List<ChatMessage> existing, List<ChatMessage> suffix) {
        if (suffix.isEmpty()) {
            return existing.size();
        }
        if (suffix.size() > existing.size()) {
            return -1;
        }
        int start = existing.size() - suffix.size();
        for (int i = 0; i < suffix.size(); i++) {
            if (!messagesEqual(existing.get(start + i), suffix.get(i))) {
                return -1;
            }
        }
        return start;
    }

    private boolean messagesEqual(ChatMessage left, ChatMessage right) {
        return ChatMessageSerializer.messageToJson(left).equals(ChatMessageSerializer.messageToJson(right));
    }
}
