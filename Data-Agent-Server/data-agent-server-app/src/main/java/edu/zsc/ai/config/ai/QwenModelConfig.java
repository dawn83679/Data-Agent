package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
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

    private StreamingChatModel buildModel(ModelEnum model, boolean enableThinking) {
        QwenChatRequestParameters.Builder params = QwenChatRequestParameters.builder()
                .enableThinking(enableThinking)
                .enableSanitizeMessages(false);
        if (enableThinking) {
            params.thinkingBudget(qwenProperties.getParameters().getThinkingBudget());
        }
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(model.getModelName())
                .defaultRequestParameters(params.build())
                .build();
    }
}
