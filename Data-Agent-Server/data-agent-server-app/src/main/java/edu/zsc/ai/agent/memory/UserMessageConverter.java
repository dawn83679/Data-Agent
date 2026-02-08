package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.*;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.common.enums.ai.MessageRoleEnum;
import edu.zsc.ai.domain.model.entity.ai.CustomAiMessage;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class UserMessageConverter {

    public MessageWithBlocks toEntity(UserMessage userMessage, Long conversationId) {
        CustomAiMessage customAiMessage = new CustomAiMessage();
        customAiMessage.setConversationId(conversationId);
        customAiMessage.setRole(MessageRoleEnum.USER.name());

        List<AiMessageBlock> blocks = new ArrayList<>();

        for (Content content : userMessage.contents()) {
            if (content instanceof ImageContent imageContent) {
                blocks.add(createBlock(MessageBlockEnum.IMAGE, JsonUtil.object2json(imageContent)));
            } else if (content instanceof VideoContent videoContent) {
                blocks.add(createBlock(MessageBlockEnum.VIDEO, JsonUtil.object2json(videoContent)));
            } else if (content instanceof PdfFileContent pdfFileContent) {
                blocks.add(createBlock(MessageBlockEnum.PDF, JsonUtil.object2json(pdfFileContent)));
            } else if (content instanceof AudioContent audioContent) {
                blocks.add(createBlock(MessageBlockEnum.AUDIO, JsonUtil.object2json(audioContent)));
            } else if (content instanceof TextContent textContent) {
                blocks.add(createBlock(MessageBlockEnum.TEXT, textContent.text()));
            }
        }

        return new MessageWithBlocks(customAiMessage, blocks);
    }


    public UserMessage toChatMessage(List<AiMessageBlock> blocks) {
        List<Content> contents = new ArrayList<>();

        for (AiMessageBlock block : blocks) {
            MessageBlockEnum blockType = MessageBlockEnum.valueOf(block.getBlockType());
            switch (blockType) {
                case IMAGE -> {
                    ImageContent imageContent = JsonUtil.json2Object(block.getContent(), ImageContent.class);
                    contents.add(imageContent);
                }
                case VIDEO -> {
                    VideoContent videoContent = JsonUtil.json2Object(block.getContent(), VideoContent.class);
                    contents.add(videoContent);
                }
                case PDF -> {
                    PdfFileContent pdfFileContent = JsonUtil.json2Object(block.getContent(), PdfFileContent.class);
                    contents.add(pdfFileContent);
                }
                case AUDIO -> {
                    AudioContent audioContent = JsonUtil.json2Object(block.getContent(), AudioContent.class);
                    contents.add(audioContent);
                }
                case TEXT -> contents.add(TextContent.from(block.getContent()));
                default -> log.debug("Ignoring block type for UserMessage: {}", blockType);
            }
        }

        return contents.isEmpty() ? UserMessage.from("") : UserMessage.from(contents);
    }

    private AiMessageBlock createBlock(MessageBlockEnum blockType, String content) {
        AiMessageBlock block = new AiMessageBlock();
        block.setBlockType(blockType.name());
        block.setContent(content);
        return block;
    }
}
