package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智谱 GLM 向量模型配置
 * 配置前缀: ai.zhipu.embedding-model
 */
@Data
@ConfigurationProperties(prefix = "ai.zhipu.embedding-model")
public class ZhipuEmbeddingProperties {

    private String apiKey;

    private String modelName = "embedding-2";

    private int dimension = 1024;
}
