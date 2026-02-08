package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConverter {

    private final AiMessageConverter aiMessageConverter;
    private final UserMessageConverter userMessageConverter;
    private final ToolResultMessageConverter toolResultMessageConverter;

    public List<MessageWithBlocks> toEntities(List<ChatMessage> messages, Long conversationId) {
        List<MessageWithBlocks> result = new ArrayList<>();

        for (ChatMessage message : messages) {
            MessageWithBlocks withBlocks = toEntity(message, conversationId);
            if (withBlocks != null) {
                result.add(withBlocks);
            }
        }

        return result;
    }

    /**
     * 将单个 LangChain4j ChatMessage 转换为 MessageWithBlocks
     */
    public MessageWithBlocks toEntity(ChatMessage message, Long conversationId) {
        if (message instanceof AiMessage aiMessage) {
            return aiMessageConverter.toEntity(aiMessage, conversationId);
        } else if (message instanceof UserMessage userMessage) {
            return userMessageConverter.toEntity(userMessage, conversationId);
        } else if (message instanceof ToolExecutionResultMessage toolResult) {
            return toolResultMessageConverter.toEntity(toolResult, conversationId);
        }
        return null;
    }

    public List<ChatMessage> toChatMessages(List<CustomAiMessage> messages,
                                            Map<Long, List<AiMessageBlock>> blocksMap) {
        return messages.stream()
                .map(msg -> toChatMessage(msg, blocksMap.getOrDefault(msg.getId(), List.of())))
                .collect(Collectors.toList());
    }

    public ChatMessage toChatMessage(CustomAiMessage customAiMessage, List<AiMessageBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("CustomAiMessage {} has no blocks", customAiMessage.getId());
            return null;
        }

        MessageRoleEnum role = MessageRoleEnum.valueOf(customAiMessage.getRole());

        return switch (role) {
            case ASSISTANT -> aiMessageConverter.toChatMessage(blocks);
            case USER -> userMessageConverter.toChatMessage(blocks);
            case TOOL -> toolResultMessageConverter.toChatMessage(blocks);
            default -> throw new IllegalStateException("Unexpected value: " + role);
        };
    }
}
