package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Zhipu GLM model configuration.
 * Split into chat-model, streaming-chat-model, embedding-model (same as Qwen).
 * Embedding is routed by {@link EmbeddingConfig} based on ai.embedding.provider.
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
                ModelEnum.GLM_5.getModelName(), buildStreamingModel(ModelEnum.GLM_5)
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.GLM_5.getModelName(), buildChatModel(ModelEnum.GLM_5)
        );
    }

    private StreamingChatModel buildStreamingModel(ModelEnum model) {
        ZhipuStreamingChatProperties.Parameters params = streamingChatProperties.getParameters();
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(streamingChatProperties.getApiKey())
                .model(model.getModelName())
                .temperature(params.getTemperature())
                .build();
    }

    private ChatModel buildChatModel(ModelEnum model) {
        ZhipuChatProperties.Parameters params = chatProperties.getParameters();
        return ZhipuAiChatModel.builder()
                .apiKey(chatProperties.getApiKey())
                .model(model.getModelName())
                .temperature(params.getTemperature())
                .build();
    }
}
