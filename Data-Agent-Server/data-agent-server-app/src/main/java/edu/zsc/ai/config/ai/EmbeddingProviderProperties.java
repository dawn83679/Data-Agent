package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding 提供方配置
 * 配置前缀: ai.embedding
 * 用于在多模型接入时选择使用哪个提供方的向量模型（千问/智谱等）
 */
@Data
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProviderProperties {

    /**
     * 提供方: qwen(千问) | zhipu(智谱)
     * 默认 qwen，与现有 DashScope 配置兼容
     */
    private String provider = "qwen";
}
