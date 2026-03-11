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
 * 智谱 GLM 模型配置
 * 与千问一致分为 chat-model、streaming-chat-model、embedding-model 三块。
 * Embedding 由 {@link EmbeddingConfig} 根据 ai.embedding.provider 统一路由。
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
                .maxToken(params.getMaxToken())
                .build();
    }

    private ChatModel buildChatModel(ModelEnum model) {
        ZhipuChatProperties.Parameters params = chatProperties.getParameters();
        return ZhipuAiChatModel.builder()
                .apiKey(chatProperties.getApiKey())
                .model(model.getModelName())
                .temperature(params.getTemperature())
                .maxToken(params.getMaxToken())
                .build();
    }
}
