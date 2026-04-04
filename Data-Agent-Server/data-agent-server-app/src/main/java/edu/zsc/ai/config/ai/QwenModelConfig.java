package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.qwen.gateway", name = "enabled", havingValue = "false")
@EnableConfigurationProperties(QwenProperties.class)
public class QwenModelConfig implements ChatModelProvider {

    private final QwenProperties qwenProperties;

    @Override
    public Map<String, StreamingChatModel> streamingChatModels() {
        return Map.of(
                ModelEnum.QWEN3_MAX.getModelName(), buildModel(ModelEnum.QWEN3_MAX, false),
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildModel(ModelEnum.QWEN3_MAX, true),
                ModelEnum.QWEN_PLUS.getModelName(), buildModel(ModelEnum.QWEN_PLUS, false)
        );
    }

    @Override
    public Map<String, ChatModel> chatModels() {
        return Map.of(
                ModelEnum.QWEN3_MAX.getModelName(), buildChatModel(ModelEnum.QWEN3_MAX),
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), buildChatModel(ModelEnum.QWEN3_MAX),
                ModelEnum.QWEN_PLUS.getModelName(), buildChatModel(ModelEnum.QWEN_PLUS)
        );
    }

    private StreamingChatModel buildModel(ModelEnum model, boolean enableThinking) {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(model.getModelName())
                .defaultRequestParameters(buildRequestParameters(enableThinking))
                .build();
    }

    private ChatModel buildChatModel(ModelEnum model) {
        return QwenChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(model.getModelName())
                .defaultRequestParameters(buildRequestParameters(false))
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
