package edu.zsc.ai.config.sys;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "auth.oauth2")
public class OAuthProperties {
    private Map<String, Registration> clients = new HashMap<>();

    @Data
    public static class Registration {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }
}
