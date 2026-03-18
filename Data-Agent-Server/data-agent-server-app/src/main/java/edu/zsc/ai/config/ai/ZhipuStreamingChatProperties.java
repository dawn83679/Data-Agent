package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zhipu GLM streaming chat model configuration.
 * Config prefix: ai.zhipu.streaming-chat-model
 */
@Data
@ConfigurationProperties(prefix = "ai.zhipu.streaming-chat-model")
public class ZhipuStreamingChatProperties {

    private String apiKey;

    private String modelName = "glm-5";

    private Parameters parameters = new Parameters();

    @Data
    public static class Parameters {

        private double temperature = 0.7;
    }
}
