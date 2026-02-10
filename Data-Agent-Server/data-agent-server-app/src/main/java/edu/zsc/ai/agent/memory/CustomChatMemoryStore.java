package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.common.constant.HitlConstants;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChatMemoryStore implements ChatMemoryStore {

    private final AiMessageService aiMessageService;
    private final AiConversationService aiConversationService;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        if (memoryId == null) {
            return List.of();
        }

        MemoryIdInfo idInfo = parseMemoryId(memoryId);
        if (idInfo == null) {
            return List.of();
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId);

        List<StoredChatMessage> stored = aiMessageService.getByConversationIdOrderByCreatedAtAsc(idInfo.conversationId);
        if (stored.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>(stored.size());
        for (StoredChatMessage s : stored) {
            try {
                ChatMessage msg = ChatMessageDeserializer.messageFromJson(s.getData());
                if (msg.type() != ChatMessageType.SYSTEM) {
                    messages.add(msg);
                }
            } catch (Exception e) {
                log.warn("Failed to deserialize message id={}, skipping", s.getId(), e);
            }
        }
        return messages;
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (memoryId == null || CollectionUtils.isEmpty(messages)) {
            return;
        }

        MemoryIdInfo idInfo = parseMemoryId(memoryId);
        if (idInfo == null) {
            return;
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId);

        aiMessageService.removeByConversationId(idInfo.conversationId);

        LocalDateTime now = LocalDateTime.now();
        List<StoredChatMessage> toSave = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            if (message.type() == ChatMessageType.SYSTEM) {
                continue;
            }
            if (message instanceof UserMessage userMsg && isHitlContinueMessage(userMsg)) {
                continue;
            }
            // TODO: Populate tokenCount from request/response tokenUsage (or ResponseMetadata); ChatMemoryStore API does not expose it—obtain from caller or streaming callback
            StoredChatMessage stored = StoredChatMessage.builder()
                    .conversationId(idInfo.conversationId())
                    .role(message.type().name())
                    .tokenCount(0)
                    .data(ChatMessageSerializer.messageToJson(message))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            toSave.add(stored);
        }

        aiMessageService.saveBatchMessages(toSave);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        if (memoryId == null) {
            return;
        }

        MemoryIdInfo idInfo = parseMemoryId(memoryId);
        if (idInfo == null) {
            return;
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId);

        int deletedCount = aiMessageService.removeByConversationId(idInfo.conversationId);
        log.debug("Deleted {} messages for conversation {}", deletedCount, idInfo.conversationId);
    }

    private MemoryIdInfo parseMemoryId(Object memoryId) {
        if (memoryId == null) {
            return null;
        }

        String id = memoryId.toString();
        String[] parts = id.split(":");

        if (parts.length != 2) {
            log.warn("Invalid memoryId format: {}. Expected format: '{{userId}}:{{conversationId}}'", id);
            return null;
        }

        try {
            Long userId = Long.parseLong(parts[0]);
            Long conversationId = Long.parseLong(parts[1]);
            return new MemoryIdInfo(userId, conversationId);
        } catch (NumberFormatException e) {
            log.warn("Invalid memoryId format: {}. Expected format: '{{userId}}:{{conversationId}}'", id, e);
            return null;
        }
    }

    private static boolean isHitlContinueMessage(UserMessage userMsg) {
        String text;
        if (userMsg.hasSingleText()) {
            text = userMsg.singleText();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Content c : userMsg.contents()) {
                if (c instanceof TextContent tc) {
                    sb.append(tc.text());
                }
            }
            text = sb.toString();
        }
        return text != null && text.trim().startsWith(HitlConstants.HITL_CONTINUE_MESSAGE_PREFIX);
    }

    private record MemoryIdInfo(Long userId, Long conversationId) {
    }
}
