package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Registers the single Qwen embedding model used by the application.
 * Runs before DashScope auto-configuration to ensure only one EmbeddingModel bean is registered.
 */
@Slf4j
@Configuration
@AutoConfigureBefore(name = "dev.langchain4j.community.dashscope.spring.DashScopeAutoConfiguration")
@EnableConfigurationProperties(DashScopeEmbeddingProperties.class)
public class EmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel(DashScopeEmbeddingProperties dashScopeProperties) {
        log.info("Using Qwen (DashScope) as embedding provider");
        return QwenEmbeddingModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }
}
