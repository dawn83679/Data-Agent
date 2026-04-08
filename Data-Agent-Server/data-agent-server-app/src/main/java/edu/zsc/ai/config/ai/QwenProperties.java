package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.qwen")
public class QwenProperties {

    private String apiKey;

<<<<<<< HEAD
    private Gateway gateway = new Gateway();
=======
    /**
     * Optional gateway/base URL for OpenAI-compatible Qwen access.
     * Bound from langchain4j.community.dashscope.streaming-chat-model.base-url.
     */
    private String baseUrl;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793

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
