package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class QwenCompatibleModelConfig implements ChatModelProvider {

    private static final String QWEN_COMPATIBLE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        return Map.of(
                ModelEnum.QWEN3_5_PLUS.getModelName(), OpenAiStreamingChatModel.builder()
                        .apiKey(qwenProperties.getApiKey())
                        .baseUrl(QWEN_COMPATIBLE_BASE_URL)
                        .modelName(ModelEnum.QWEN3_5_PLUS.getModelName())
                        .build()
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.QWEN3_5_PLUS.getModelName(), OpenAiChatModel.builder()
                        .apiKey(qwenProperties.getApiKey())
                        .baseUrl(QWEN_COMPATIBLE_BASE_URL)
                        .modelName(ModelEnum.QWEN3_5_PLUS.getModelName())
                        .build()
        );
    }
}
