package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.qwen")
public class QwenProperties {

    private String apiKey;

    private Gateway gateway = new Gateway();

    private Parameters parameters = new Parameters();

    @Data
    public static class Gateway {

        private boolean enabled = true;

        /**
         * Optional gateway/base URL for OpenAI-compatible Qwen access.
         */
        private String baseUrl = "https://coding.dashscope.aliyuncs.com/v1";
    }

    @Data
    public static class Parameters {

        private int thinkingBudget = 1000;
    }
}
