package edu.zsc.ai.service.impl.manager;

import edu.zsc.ai.config.CompressContextConfig;
import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;
import edu.zsc.ai.enums.ai.message.MessageRoleEnum;
import edu.zsc.ai.model.dto.request.ai.message.SaveMessageRequest;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.service.ai.AiMessageService;
import edu.zsc.ai.service.ai.manager.ContextManager;
import edu.zsc.ai.util.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ContextManagerImpl implements ContextManager {

    @Autowired
    private AiMessageService aiMessageService;

    @Autowired
    @Qualifier("CompressContextChatClient")
    private ChatClient compressContextChatClient;

    @Autowired
    private CompressContextConfig compressContextConfig;

    @Override
    public HistoryContextResponse getContextForAI(Long conversationId) {
        return aiMessageService.getAIContext(conversationId);
    }

    @Override
    public HistoryContextResponse compressContext(Long conversationId, HistoryContextResponse context) {
        log.info("Compressing context for conversation {}", conversationId);
        String systemPrompt = PromptLoader.getCompressSystemPrompt();
        List<Message> needToCompressMessages = context.getMessages();
        List<Message> compressedMessages = new ArrayList<>(needToCompressMessages.size() + 1);
        compressedMessages.add(new SystemMessage(systemPrompt));
        compressedMessages.addAll(needToCompressMessages);

        String compressResult = compressContextChatClient.prompt().messages(compressedMessages).call().content();
        //TODO: handle compressResult null or empty
        aiMessageService.saveMessage(new SaveMessageRequest(conversationId, compressResult,
                MessageRoleEnum.ASSISTANT.name(), compressContextConfig.getMaxToken(),
                MessageBlockTypeEnum.SUMMARY.name(), null));


        // Return compressed context with metadata
        return HistoryContextResponse.builder()
                .messages(List.of(new AssistantMessage(compressResult)))
                .totalTokenCount(compressContextConfig.getMaxToken())
                .build();
    }
}
