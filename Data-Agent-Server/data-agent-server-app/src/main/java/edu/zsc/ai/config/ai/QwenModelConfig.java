package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.qwen.gateway", name = "enabled", havingValue = "false")
@EnableConfigurationProperties(QwenProperties.class)
public class QwenModelConfig implements ChatModelProvider {

    private final AiModelCatalog aiModelCatalog;
    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        Map<String, StreamingChatModel> models = new LinkedHashMap<>();
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
            models.put(model.getModelName(), buildModel(model));
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

    private StreamingChatModel buildModel(AiModelProperties.ModelDefinition model) {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(model.getApiModelName())
                .defaultRequestParameters(buildRequestParameters(model.isSupportThinking()))
                .build();
    }

    private ChatModel buildChatModel(AiModelProperties.ModelDefinition model) {
        return QwenChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(model.getApiModelName())
                .defaultRequestParameters(buildRequestParameters(model.isSupportThinking()))
                .build();
    }

    private QwenChatRequestParameters buildRequestParameters(boolean enableThinking) {
        QwenChatRequestParameters.Builder params = QwenChatRequestParameters.builder()
                .enableThinking(enableThinking)
                .enableSanitizeMessages(false);
        if (enableThinking) {
            params.thinkingBudget(qwenProperties.getParameters().getThinkingBudget());
        }
        return params.build();
    }
}
