package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures shared StreamingChatModel beans for all supported models.
 * ReActAgent instances are created per request in ChatServiceImpl.
 */
@Configuration
public class MultiModelAgentConfig {

    private static final int THINKING_BUDGET = 1000;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}")
    private String apiKey;

    @Bean("streamingChatModelQwen3Max")
    public StreamingChatModel streamingChatModelQwen3Max() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                // Disable QwenHelper message sanitization to preserve full ReAct + tool context
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    /** Same API model as qwen3-max, but with thinking enabled (for frontend option qwen3-max-thinking). */
    @Bean("streamingChatModelQwen3MaxThinking")
    public StreamingChatModel streamingChatModelQwen3MaxThinking() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(true)
                                .thinkingBudget(THINKING_BUDGET)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwenPlus")
    public StreamingChatModel streamingChatModelQwenPlus() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(ModelEnum.QWEN_PLUS.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }
}
