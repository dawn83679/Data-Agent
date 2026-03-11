package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiniMax 模型配置（参照 Qwen 单配置块）
 * 配置前缀: ai.minimax
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

        private double temperature = 0.7;

        private int maxToken = 4096;
    }
}
