package edu.zsc.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.context.compress")
public class CompressContextConfig {

    private int maxToken;
}
