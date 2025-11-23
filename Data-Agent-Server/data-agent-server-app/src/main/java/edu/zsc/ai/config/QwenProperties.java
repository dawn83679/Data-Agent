package edu.zsc.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Qwen model
 *
 * @author zgq
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.dashscope")
public class QwenProperties {

    /**
     * API key for DashScope
     */
    private String apiKey;

    /**
     * Base URL for DashScope API
     */
    private String baseUrl;

}