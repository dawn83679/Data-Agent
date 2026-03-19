package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Qwen 模型配置（OpenAI 兼容接口，默认启用）
 * 切换为原生 DashScope SDK 时，在 yml 中设置 ai.qwen.mode=native
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(QwenProperties.class)
@ConditionalOnProperty(name = "ai.qwen.mode", havingValue = "openai", matchIfMissing = true)
public class QwenOpenAiModelConfig implements ChatModelProvider {

    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        return Map.of(
                ModelEnum.QWEN3_5_PLUS.getModelName(), buildStreaming(ModelEnum.QWEN3_5_PLUS),
                ModelEnum.QWEN3_MAX.getModelName(), buildStreaming(ModelEnum.QWEN3_MAX),
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildStreaming(ModelEnum.QWEN3_MAX)
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.QWEN3_5_PLUS.getModelName(), buildChat(ModelEnum.QWEN3_5_PLUS),
                ModelEnum.QWEN3_MAX.getModelName(), buildChat(ModelEnum.QWEN3_MAX),
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildChat(ModelEnum.QWEN3_MAX)
        );
    }

    private StreamingChatModel buildStreaming(ModelEnum model) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getBaseUrl())
                .modelName(model.getModelName())
                .build();
    }

    private ChatModel buildChat(ModelEnum model) {
        return OpenAiChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getBaseUrl())
                .modelName(model.getModelName())
                .build();
    }
}
