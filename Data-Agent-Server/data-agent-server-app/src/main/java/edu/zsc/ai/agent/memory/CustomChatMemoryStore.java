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

        return compressor.compressIfNeeded(idInfo.conversationId(), idInfo.modelName(), messages);
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

        List<ChatMessage> nonSystemMessages = messages.stream()
                .filter(m -> m.type() != ChatMessageType.SYSTEM)
                .toList();

        List<ChatMessage> toPersist = compressor.compressIfNeeded(
                idInfo.conversationId(), idInfo.modelName(), nonSystemMessages);

        aiMessageService.replaceConversationMessages(idInfo.conversationId(), toPersist);
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
