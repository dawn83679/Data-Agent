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
<<<<<<< HEAD
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiEmbeddingProperties.class)
=======
 * Runs before DashScope auto-configuration to ensure only one EmbeddingModel bean is registered.
 */
@Slf4j
@Configuration
@AutoConfigureBefore(name = "dev.langchain4j.community.dashscope.spring.DashScopeAutoConfiguration")
@EnableConfigurationProperties(DashScopeEmbeddingProperties.class)
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
public class EmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingModel.class)
<<<<<<< HEAD
    public EmbeddingModel embeddingModel(AiEmbeddingProperties embeddingProperties) {
        log.info("Using Qwen (DashScope) as embedding provider");
        return QwenEmbeddingModel.builder()
                .apiKey(embeddingProperties.getApiKey())
                .modelName(embeddingProperties.getModelName())
=======
    public EmbeddingModel embeddingModel(DashScopeEmbeddingProperties dashScopeProperties) {
        log.info("Using Qwen (DashScope) as embedding provider");
        return QwenEmbeddingModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
                .build();
    }
}
