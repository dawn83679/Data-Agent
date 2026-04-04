package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Registers the single Qwen embedding model used by the application.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiEmbeddingProperties.class)
public class EmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel(AiEmbeddingProperties embeddingProperties) {
        log.info("Using Qwen (DashScope) as embedding provider");
        return QwenEmbeddingModel.builder()
                .apiKey(embeddingProperties.getApiKey())
                .modelName(embeddingProperties.getModelName())
                .build();
    }
}
