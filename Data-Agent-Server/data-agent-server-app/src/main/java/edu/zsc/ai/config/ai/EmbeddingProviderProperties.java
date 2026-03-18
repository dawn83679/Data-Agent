package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding provider configuration.
 * Config prefix: ai.embedding
 * Selects which provider's embedding model to use when multiple models are configured.
 */
@Data
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProviderProperties {

    /**
     * Provider: qwen | zhipu. Defaults to qwen (compatible with existing DashScope config).
     */
    private String provider = "qwen";
}
