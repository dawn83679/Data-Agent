package edu.zsc.ai.config.ai;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 模型统一配置
 * 根据 ai.embedding.provider 选择千问或智谱向量模型，避免多模型接入时 Bean 冲突。
 * 优先于 DashScope 自动配置执行，确保只注册一个 EmbeddingModel Bean。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@AutoConfigureBefore(name = "dev.langchain4j.community.dashscope.spring.DashScopeAutoConfiguration")
@EnableConfigurationProperties({
        EmbeddingProviderProperties.class,
        DashScopeEmbeddingProperties.class,
        ZhipuEmbeddingProperties.class
})
public class EmbeddingConfig {

    private final EmbeddingProviderProperties providerProperties;
    private final DashScopeEmbeddingProperties dashScopeProperties;
    private final ZhipuEmbeddingProperties zhipuProperties;

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        String provider = providerProperties.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = "qwen";
        }
        return switch (provider.toLowerCase()) {
            case "zhipu" -> buildZhipuEmbeddingModel();
            case "qwen", "dashscope" -> buildQwenEmbeddingModel();
            default -> {
                log.warn("Unknown embedding provider '{}', falling back to qwen", provider);
                yield buildQwenEmbeddingModel();
            }
        };
    }

    private EmbeddingModel buildQwenEmbeddingModel() {
        log.info("Using Qwen (DashScope) as embedding provider");
        return QwenEmbeddingModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }

    private EmbeddingModel buildZhipuEmbeddingModel() {
        log.info("Using Zhipu as embedding provider");
        return ZhipuAiEmbeddingModel.builder()
                .apiKey(zhipuProperties.getApiKey())
                .model(zhipuProperties.getModelName())
                .build();
    }
}
