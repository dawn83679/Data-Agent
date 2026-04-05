package edu.zsc.ai.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private boolean enabled = true;

    private Retrieval retrieval = new Retrieval();

    @Data
    public static class Retrieval {

        private double minScore = 0.72;
    }
}
