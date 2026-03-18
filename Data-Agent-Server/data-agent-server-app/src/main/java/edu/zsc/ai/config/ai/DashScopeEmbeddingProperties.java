package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Qwen (DashScope) embedding model configuration.
 * Config prefix: langchain4j.community.dashscope.embedding-model
 */
@Data
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.embedding-model")
public class DashScopeEmbeddingProperties {

    private String apiKey;

    private String modelName = "text-embedding-v2";

    private int dimension = 1024;
}
