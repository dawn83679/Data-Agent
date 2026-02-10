package edu.zsc.ai.common.converter.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.domain.model.dto.response.ai.ConversationMessageResponse;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts stored chat message + deserialized ChatMessage to API response DTO.
 */
public final class StoredMessageToResponseConverter {

    private StoredMessageToResponseConverter() {
    }

    /**
     * Converts one stored message (with its deserialized payload) to ConversationMessageResponse.
     * Role is normalized to "user" or "assistant" for frontend.
     */
    public static ConversationMessageResponse toResponse(StoredChatMessage stored, ChatMessage message) {
        String role = MessageRoleEnum.fromBackendType(message.type().name()).getValue();
        String content;
        List<ChatResponseBlock> blocks;

        if (message instanceof UserMessage userMsg) {
            content = userMessageContent(userMsg);
            blocks = List.of(ChatResponseBlock.text(content));
        } else if (message instanceof AiMessage aiMsg) {
            content = StringUtils.defaultString(aiMsg.text());
            blocks = aiMessageBlocks(aiMsg);
        } else if (message instanceof ToolExecutionResultMessage toolMsg) {
            content = StringUtils.defaultString(toolMsg.text());
            boolean isError = Boolean.TRUE.equals(toolMsg.isError());
            blocks = List.of(ChatResponseBlock.toolResult(
                    toolMsg.id(),
                    toolMsg.toolName(),
                    toolMsg.text(),
                    isError));
        } else {
            content = "";
            blocks = List.of();
        }

        return ConversationMessageResponse.builder()
                .id(stored.getId() != null ? stored.getId().toString() : null)
                .role(role)
                .content(StringUtils.defaultString(content))
                .blocks(blocks)
                .createdAt(stored.getCreatedAt())
                .build();
    }

    private static String userMessageContent(UserMessage userMsg) {
        if (CollectionUtils.isEmpty(userMsg.contents())) {
            return "";
        }
        return userMsg.contents().stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .collect(Collectors.joining("\n"));
    }

    private static List<ChatResponseBlock> aiMessageBlocks(AiMessage aiMsg) {
        List<ChatResponseBlock> out = new ArrayList<>();
        if (StringUtils.isNotBlank(aiMsg.thinking())) {
            out.add(ChatResponseBlock.thought(aiMsg.thinking()));
        }
        if (StringUtils.isNotBlank(aiMsg.text())) {
            out.add(ChatResponseBlock.text(aiMsg.text()));
        }
        if (CollectionUtils.isNotEmpty(aiMsg.toolExecutionRequests())) {
            for (var req : aiMsg.toolExecutionRequests()) {
                out.add(ChatResponseBlock.toolCall(
                        req.id(),
                        StringUtils.defaultString(req.name()),
                        StringUtils.defaultString(req.arguments())
                ));
            }
        }
        return CollectionUtils.isEmpty(out) ? List.of() : out;
    }
}
