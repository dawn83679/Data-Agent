package edu.zsc.ai.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.streaming-chat-model")
public class QwenProperties {

    private String apiKey;

    private String baseUrl;

    private Parameters parameters = new Parameters();

    @Data
    public static class Parameters {

        private boolean enableThinking = true;

        private int thinkingBudget = 1000;
    }
}
