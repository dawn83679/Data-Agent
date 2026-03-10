package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.ai.CompressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompressionServiceImpl implements CompressionService {

    private final Map<String, ChatModel> chatModelsByName;

    @Override
    public String compress(List<ChatMessage> messages) {
        String serializedHistory = ChatMessageSerializer.messagesToJson(messages);
        String template = PromptConfig.getPrompt(PromptEnum.COMPRESSION);
        String prompt = String.format(template, serializedHistory);

        ChatModel model = chatModelsByName.get(ModelEnum.QWEN_PLUS.getModelName());
        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .build();

        ChatResponse response = model.chat(request);
        String summary = response.aiMessage().text();

        log.debug("Compressed {} messages into summary ({} chars)", messages.size(), summary.length());
        return summary;
    }
}
