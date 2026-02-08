package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class ToolResultMessageConverter {

    public MessageWithBlocks toEntity(ToolExecutionResultMessage toolResult, Long conversationId) {
        CustomAiMessage customAiMessage = new CustomAiMessage();
        customAiMessage.setConversationId(conversationId);
        customAiMessage.setRole(MessageRoleEnum.TOOL.name());

        String content = JsonUtil.object2json(toolResult);

        AiMessageBlock resultBlock = new AiMessageBlock();
        resultBlock.setBlockType(MessageBlockEnum.TOOL_RESULT.name());
        resultBlock.setContent(content);

        return new MessageWithBlocks(customAiMessage, List.of(resultBlock));
    }

    public ToolExecutionResultMessage toChatMessage(List<AiMessageBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("ToolExecutionResultMessage has no blocks");
            return ToolExecutionResultMessage.from("", "", "");
        }
        return JsonUtil.json2Object(blocks.get(0).getContent(), ToolExecutionResultMessage.class);
    }
}
