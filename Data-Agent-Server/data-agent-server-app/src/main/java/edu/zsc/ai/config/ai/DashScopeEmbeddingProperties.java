package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 千问（DashScope）向量模型配置
 * 配置前缀与 langchain4j DashScope 一致: langchain4j.community.dashscope.embedding-model
 */
@Data
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.embedding-model")
public class DashScopeEmbeddingProperties {

    private String apiKey;

    private String modelName = "text-embedding-v2";

    private int dimension = 1024;
}
