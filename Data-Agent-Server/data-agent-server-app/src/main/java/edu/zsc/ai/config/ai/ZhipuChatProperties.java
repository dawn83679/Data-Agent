package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智谱 GLM 非流式对话模型配置
 * 配置前缀: ai.zhipu.chat-model
 */
@Data
@ConfigurationProperties(prefix = "ai.zhipu.chat-model")
public class ZhipuChatProperties {

    private String apiKey;

    private String modelName = "glm-5";

    private Parameters parameters = new Parameters();

    @Data
    public static class Parameters {

        private double temperature = 0.7;

        private int maxToken = 4096;
    }
}
