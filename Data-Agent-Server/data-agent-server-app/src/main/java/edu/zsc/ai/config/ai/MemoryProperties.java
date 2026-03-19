package edu.zsc.ai.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private boolean enabled = true;

    private Embedding embedding = new Embedding();

    private Retrieval retrieval = new Retrieval();

    private Maintenance maintenance = new Maintenance();

    @Data
    public static class Embedding {

        private int dimension = 1024;
    }

    @Data
    public static class Retrieval {

        private double minScore = 0.72;
    }

    @Data
    public static class Maintenance {

        private boolean enabled = true;

        private long fixedDelayMs = 3_600_000L;

        private boolean archiveExpiredEnabled = true;

        private boolean hideDuplicateEnabled = true;
    }
}
