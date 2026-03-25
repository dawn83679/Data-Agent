package edu.zsc.ai.common.converter.ai;

import dev.langchain4j.data.message.*;
import edu.zsc.ai.agent.memory.ChatMemoryCompressor;
import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.common.enums.ai.MessageStatusEnum;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.dto.response.ai.ConversationMessageResponse;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts stored chat message + deserialized ChatMessage to API response DTO.
 */
@Component
@RequiredArgsConstructor
public class StoredMessageToResponseConverter {

    /**
     * Converts one stored message (with its deserialized payload) to ConversationMessageResponse.
     * Role is normalized to "user" or "assistant" for frontend.
     */
    public ConversationMessageResponse toResponse(StoredChatMessage stored, ChatMessage message) {
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
                .messageStatus(resolveMessageStatus(stored.getStatus(), message))
                .createdAt(stored.getCreatedAt())
                .build();
    }

    private String resolveMessageStatus(Integer statusCode, ChatMessage message) {
        if (statusCode != null
                && statusCode == MessageStatusEnum.COMPRESSED.getCode()
                && ChatMemoryCompressor.isSummaryMessage(message)) {
            return MessageStatusEnum.COMPRESSION_SUMMARY.name();
        }
        if (statusCode == null) {
            return MessageStatusEnum.NORMAL.name();
        }
        return MessageStatusEnum.fromCode(statusCode).name();
    }

    private String userMessageContent(UserMessage userMsg) {
        if (CollectionUtils.isEmpty(userMsg.contents())) {
            return "";
        }
        String content = userMsg.contents().stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .collect(Collectors.joining("\n"));
        return MemoryUtil.stripInjectedWrapper(content);
    }

    private List<ChatResponseBlock> aiMessageBlocks(AiMessage aiMsg) {
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
                        StringUtils.defaultString(req.arguments())));
            }
        }
        return CollectionUtils.isEmpty(out) ? List.of() : out;
    }
}
