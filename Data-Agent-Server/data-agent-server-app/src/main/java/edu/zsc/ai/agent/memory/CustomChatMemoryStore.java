package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChatMemoryStore implements ChatMemoryStore {

    private final AiMessageService aiMessageService;
    private final AiConversationService aiConversationService;
    private final ChatMemoryCompressor compressor;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        MemoryIdInfo idInfo = MemoryIdUtil.parse(memoryId);
        if (idInfo == null) {
            return List.of();
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId());

        List<StoredChatMessage> stored = aiMessageService.getActiveByConversationIdOrderByCreatedAtAsc(idInfo.conversationId());
        if (stored.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = stored.stream()
                .map(item -> ChatMessageDeserializer.messageFromJson(item.getData()))
                .toList();
        List<ChatMessage> ordered = moveCompactionContextToFront(messages);

        return compressor.compressIfNeeded(idInfo.conversationId(), idInfo.modelName(), ordered);
    }

    static List<ChatMessage> moveCompactionContextToFront(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return messages;
        }
        int compactionContextIdx = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (ChatMemoryCompressor.isCompactionContextMessage(messages.get(i))) {
                compactionContextIdx = i;
                break;
            }
        }
        if (compactionContextIdx <= 0) {
            return messages;
        }
        List<ChatMessage> reordered = new ArrayList<>(messages.size());
        reordered.add(messages.get(compactionContextIdx));
        for (int i = 0; i < messages.size(); i++) {
            if (i != compactionContextIdx) {
                reordered.add(messages.get(i));
            }
        }
        return reordered;
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        MemoryIdInfo idInfo = MemoryIdUtil.parse(memoryId);
        if (idInfo == null) {
            return;
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId());

        List<ChatMessage> persistableMessages = messages.stream()
                .filter(CustomChatMemoryStore::shouldPersistRuntimeMessage)
                .toList();

        List<ChatMessage> toPersist = compressor.compressIfNeeded(
                idInfo.conversationId(), idInfo.modelName(), persistableMessages);

        aiMessageService.replaceConversationMessages(idInfo.conversationId(), toPersist);
    }

    static boolean shouldPersistRuntimeMessage(ChatMessage message) {
        return message.type() != ChatMessageType.SYSTEM
                || ChatMemoryCompressor.isCompactionContextMessage(message);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        MemoryIdInfo idInfo = MemoryIdUtil.parse(memoryId);
        if (idInfo == null) {
            return;
        }

        aiConversationService.checkAccess(idInfo.userId(), idInfo.conversationId());

        int deletedCount = aiMessageService.removeByConversationId(idInfo.conversationId());
        log.debug("Deleted {} messages for conversation {}", deletedCount, idInfo.conversationId());
    }
}
