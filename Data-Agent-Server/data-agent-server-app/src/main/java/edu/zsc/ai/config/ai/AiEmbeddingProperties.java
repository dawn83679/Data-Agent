package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding model configuration shared by the Qwen embedding client and pgvector store.
 */
@Data
@ConfigurationProperties(prefix = "ai.embedding")
public class AiEmbeddingProperties {

    private String apiKey;

    private String modelName = "text-embedding-v2";

    private int dimension = 1024;
}
