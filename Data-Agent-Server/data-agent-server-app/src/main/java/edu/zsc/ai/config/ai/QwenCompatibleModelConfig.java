package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.qwen.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(QwenProperties.class)
public class QwenCompatibleModelConfig implements ChatModelProvider {

    private final AiModelCatalog aiModelCatalog;
    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        Map<String, StreamingChatModel> models = new LinkedHashMap<>();
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
            models.put(model.getModelName(), buildStreamingModel(model));
        }
        return models;
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        Map<String, ChatModel> models = new LinkedHashMap<>();
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
            models.put(model.getModelName(), buildChatModel(model));
        }
        return models;
    }

    private StreamingChatModel buildStreamingModel(AiModelProperties.ModelDefinition model) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getGateway().getBaseUrl())
                .modelName(model.getApiModelName())
                .defaultRequestParameters(buildRequestParameters(model.isSupportThinking()))
                .build();
    }

    private ChatModel buildChatModel(AiModelProperties.ModelDefinition model) {
        return OpenAiChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getGateway().getBaseUrl())
                .modelName(model.getApiModelName())
                .defaultRequestParameters(buildRequestParameters(model.isSupportThinking()))
                .build();
    }

    private OpenAiChatRequestParameters buildRequestParameters(boolean enableThinking) {
        OpenAiChatRequestParameters.Builder params = OpenAiChatRequestParameters.builder();
        if (enableThinking) {
            Map<String, Object> customParameters = new LinkedHashMap<>();
            customParameters.put("enable_thinking", true);
            customParameters.put("thinking_budget", qwenProperties.getParameters().getThinkingBudget());
            params.customParameters(customParameters);
        }
        return params.build();
    }
}
