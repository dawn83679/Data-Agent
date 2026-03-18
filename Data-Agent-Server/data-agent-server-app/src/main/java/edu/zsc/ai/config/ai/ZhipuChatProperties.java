package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zhipu GLM non-streaming chat model configuration.
 * Config prefix: ai.zhipu.chat-model
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
    }
}
