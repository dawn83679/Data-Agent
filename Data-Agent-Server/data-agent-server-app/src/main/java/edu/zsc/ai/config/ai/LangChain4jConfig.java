package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import edu.zsc.ai.common.enums.ai.ModelContextLimitEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LangChain4jConfig {

    private final TokenCountEstimator tokenCountEstimator;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name:qwen3-max}")
    private String streamingModelName;

    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryProvider chatMemoryProvider() {
        int memoryThreshold = ModelContextLimitEnum.fromModelName(streamingModelName).getMemoryThreshold();
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(memoryThreshold, tokenCountEstimator)
                .build();
    }
}
