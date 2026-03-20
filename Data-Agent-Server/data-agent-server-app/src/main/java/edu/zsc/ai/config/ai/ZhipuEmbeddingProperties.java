package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zhipu GLM embedding model configuration.
 * Config prefix: ai.zhipu.embedding-model
 */
@Data
@ConfigurationProperties(prefix = "ai.zhipu.embedding-model")
public class ZhipuEmbeddingProperties {

    private String apiKey;

    private String baseUrl;

    private String modelName = "embedding-2";

    private int dimension = 1024;
}
