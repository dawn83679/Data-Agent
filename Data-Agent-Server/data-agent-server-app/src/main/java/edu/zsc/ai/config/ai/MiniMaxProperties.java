package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiniMax model configuration (following Qwen config pattern).
 * Config prefix: ai.minimax
 */
@Data
@ConfigurationProperties(prefix = "ai.minimax")
public class MiniMaxProperties {

    private String apiKey;

    private String baseUrl = "https://api.minimaxi.com/v1";

    private String modelName = "MiniMax-M2.5";

    private Parameters parameters = new Parameters();

    @Data
    public static class Parameters {

        private double temperature = 0.1;
    }
}
