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

<<<<<<< HEAD
    private final AiModelCatalog aiModelCatalog;
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        Map<String, StreamingChatModel> models = new LinkedHashMap<>();
<<<<<<< HEAD
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
            models.put(model.getModelName(), buildStreamingModel(model));
        }
=======
        models.put(ModelEnum.QWEN3_5_PLUS.getModelName(), buildStreamingModel(ModelEnum.QWEN3_5_PLUS, false));
        models.put(ModelEnum.QWEN3_MAX.getModelName(), buildStreamingModel(ModelEnum.QWEN3_MAX, false));
        models.put(ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildStreamingModel(ModelEnum.QWEN3_MAX, true));
        models.put(ModelEnum.QWEN_PLUS.getModelName(), buildStreamingModel(ModelEnum.QWEN_PLUS, false));
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        return models;
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        Map<String, ChatModel> models = new LinkedHashMap<>();
<<<<<<< HEAD
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
=======
        models.put(ModelEnum.QWEN3_5_PLUS.getModelName(), buildChatModel(ModelEnum.QWEN3_5_PLUS, false));
        models.put(ModelEnum.QWEN3_MAX.getModelName(), buildChatModel(ModelEnum.QWEN3_MAX, false));
        models.put(ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildChatModel(ModelEnum.QWEN3_MAX, true));
        models.put(ModelEnum.QWEN_PLUS.getModelName(), buildChatModel(ModelEnum.QWEN_PLUS, false));
        return models;
    }

    private StreamingChatModel buildStreamingModel(ModelEnum actualModel, boolean enableThinking) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getBaseUrl())
                .modelName(actualModel.getModelName())
                .defaultRequestParameters(buildRequestParameters(enableThinking))
                .build();
    }

    private ChatModel buildChatModel(ModelEnum actualModel, boolean enableThinking) {
        return OpenAiChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .baseUrl(qwenProperties.getBaseUrl())
                .modelName(actualModel.getModelName())
                .defaultRequestParameters(buildRequestParameters(enableThinking))
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
