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
 * 智谱 GLM 模型配置（OpenAI 兼容接口）
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        ZhipuChatProperties.class,
        ZhipuStreamingChatProperties.class
})
public class ZhipuModelConfig implements ChatModelProvider {

    private final ZhipuChatProperties chatProperties;
    private final ZhipuStreamingChatProperties streamingChatProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        return Map.of(
                ModelEnum.GLM_5.getModelName(), OpenAiStreamingChatModel.builder()
                        .apiKey(streamingChatProperties.getApiKey())
                        .baseUrl(streamingChatProperties.getBaseUrl())
                        .modelName(resolveModelName(streamingChatProperties.getModelName()))
                        .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                                .temperature(streamingChatProperties.getParameters().getTemperature())
                                .maxCompletionTokens(streamingChatProperties.getParameters().getMaxToken())
                                .build())
                        .build()
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.GLM_5.getModelName(), OpenAiChatModel.builder()
                        .apiKey(chatProperties.getApiKey())
                        .baseUrl(chatProperties.getBaseUrl())
                        .modelName(resolveModelName(chatProperties.getModelName()))
                        .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                                .temperature(chatProperties.getParameters().getTemperature())
                                .maxCompletionTokens(chatProperties.getParameters().getMaxToken())
                                .build())
                        .build()
        );
    }

    private String resolveModelName(String configuredModelName) {
        if (configuredModelName == null || configuredModelName.isBlank()) {
            return ModelEnum.GLM_5.getModelName();
        }
        return configuredModelName.trim();
    }
}
