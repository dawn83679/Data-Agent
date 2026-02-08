package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageBlockService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChatMemoryStore implements ChatMemoryStore {

    private final AiMessageService aiMessageService;
    private final AiMessageBlockService aiMessageBlockService;
    private final AiConversationService aiConversationService;
    private final ChatMessageConverter chatMessageConverter;

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

        List<CustomAiMessage> messages = aiMessageService.getByConversationIdOrderByCreatedAtAsc(idInfo.conversationId);
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Long> messageIds = messages.stream()
                .map(CustomAiMessage::getId)
                .collect(Collectors.toList());

        List<AiMessageBlock> allBlocks = aiMessageBlockService.getByMessageIds(messageIds);

        Map<Long, List<AiMessageBlock>> blocksMap = allBlocks.stream()
                .collect(Collectors.groupingBy(AiMessageBlock::getMessageId));

        return chatMessageConverter.toChatMessages(messages, blocksMap);


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

        List<MessageWithBlocks> messagesWithBlocks =
                chatMessageConverter.toEntities(messages, idInfo.conversationId);

        List<CustomAiMessage> customAiMessages = messagesWithBlocks.stream()
                .map(MessageWithBlocks::customAiMessage)
                .collect(Collectors.toList());

        aiMessageService.saveBatchMessages(customAiMessages);

        List<AiMessageBlock> allBlocks = new ArrayList<>();
        for (int i = 0; i < messagesWithBlocks.size(); i++) {
            CustomAiMessage savedMessage = customAiMessages.get(i);
            List<AiMessageBlock> blocks = messagesWithBlocks.get(i).blocks();
            for (AiMessageBlock block : blocks) {
                block.setMessageId(savedMessage.getId());
            }
            allBlocks.addAll(blocks);
        }

        aiMessageBlockService.saveBatchBlocks(allBlocks);
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

        Long userId = Long.parseLong(parts[0]);
        Long conversationId = Long.parseLong(parts[1]);
        return new MemoryIdInfo(userId, conversationId);
    }

    private record MemoryIdInfo(Long userId, Long conversationId) {
    }
}
