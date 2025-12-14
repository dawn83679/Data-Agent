package edu.zsc.ai.config.sys;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.refresh-token")
public class RefreshTokenProperties {
    private long expireSeconds = 2592000; // Default 30 days
}
