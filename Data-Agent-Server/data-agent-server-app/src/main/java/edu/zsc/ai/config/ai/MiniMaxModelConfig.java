package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * MiniMax M2.5 模型配置（参照 Qwen 接入方式）
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MiniMaxProperties.class)
public class MiniMaxModelConfig implements ChatModelProvider {

    private final MiniMaxProperties miniMaxProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        return Map.of(
                ModelEnum.MINIMAX_M2_5.getModelName(), buildStreamingModel()
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.MINIMAX_M2_5.getModelName(), buildChatModel()
        );
    }

    private StreamingChatModel buildStreamingModel() {
        MiniMaxProperties.Parameters params = miniMaxProperties.getParameters();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(miniMaxProperties.getBaseUrl())
                .apiKey(miniMaxProperties.getApiKey())
                .modelName(miniMaxProperties.getModelName())
                .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                        .temperature(params.getTemperature())
                        .maxOutputTokens(params.getMaxToken())
                        .build())
                .build();
    }

    private ChatModel buildChatModel() {
        MiniMaxProperties.Parameters params = miniMaxProperties.getParameters();
        return OpenAiChatModel.builder()
                .baseUrl(miniMaxProperties.getBaseUrl())
                .apiKey(miniMaxProperties.getApiKey())
                .modelName(miniMaxProperties.getModelName())
                .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                        .temperature(params.getTemperature())
                        .maxOutputTokens(params.getMaxToken())
                        .build())
                .build();
    }
}
