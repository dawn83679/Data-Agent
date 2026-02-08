package edu.zsc.ai.agent.memory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class AiMessageConverter {

    public MessageWithBlocks toEntity(AiMessage aiMessage, Long conversationId) {
        CustomAiMessage customAiMessage = new CustomAiMessage();
        customAiMessage.setConversationId(conversationId);
        customAiMessage.setRole(MessageRoleEnum.ASSISTANT.name());

        List<AiMessageBlock> blocks = new ArrayList<>();

        if (StringUtils.isNoneEmpty(aiMessage.thinking())) {
            blocks.add(createBlock(MessageBlockEnum.THOUGHT, aiMessage.thinking()));
        }

        if (StringUtils.isNoneEmpty(aiMessage.text())) {
            blocks.add(createBlock(MessageBlockEnum.TEXT, aiMessage.text()));
        }

        if (aiMessage.hasToolExecutionRequests()) {
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                String toolJson = JsonUtil.object2json(toolExecutionRequest);
                blocks.add(createBlock(MessageBlockEnum.TOOL_CALL, toolJson));
            }
        }

        return new MessageWithBlocks(customAiMessage, blocks);
    }

    public AiMessage toChatMessage(List<AiMessageBlock> blocks) {
        String text = null;
        String thinking = null;
        List<ToolExecutionRequest> toolRequests = new ArrayList<>();

        for (AiMessageBlock block : blocks) {
            MessageBlockEnum blockType = MessageBlockEnum.valueOf(block.getBlockType());
            switch (blockType) {
                case TEXT -> text = block.getContent();
                case THOUGHT -> thinking = block.getContent();
                case TOOL_CALL ->
                        toolRequests.add(JsonUtil.json2Object(block.getContent(), ToolExecutionRequest.class));
                default -> log.debug("Ignoring block type for AiMessage: {}", blockType);
            }
        }

        AiMessage.Builder builder = AiMessage.builder();

        if (text != null) {
            builder.text(text);
        }
        if (CollectionUtils.isNotEmpty(toolRequests)) {
            builder.toolExecutionRequests(toolRequests);
        }
        if (thinking != null) {
            builder.thinking(thinking);
        }

        return builder.build();
    }

    private AiMessageBlock createBlock(MessageBlockEnum blockType, String content) {
        AiMessageBlock block = new AiMessageBlock();
        block.setBlockType(blockType.name());
        block.setContent(content);
        return block;
    }
}
