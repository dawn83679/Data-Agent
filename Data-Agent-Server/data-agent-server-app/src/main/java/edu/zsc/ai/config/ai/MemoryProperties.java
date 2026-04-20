package edu.zsc.ai.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private boolean enabled = true;

    private Retrieval retrieval = new Retrieval();

    private Autowrite autowrite = new Autowrite();

    @Data
    public static class Retrieval {

        private double minScore = 0.72;
    }

    /**
     * Background conversation memory extraction (LLM) and optional merge/dedup tuning.
     */
    @Data
    public static class Autowrite {

        /** When true, {@code pg_try_advisory_xact_lock} wraps each extraction (multi-instance safe). */
        private boolean advisoryLockEnabled = true;

        /** If true, CREATE may merge into an existing enabled memory when embedding similarity is high. */
        private boolean vectorMergeEnabled = true;

        /** Minimum cosine similarity to treat two memories as duplicates during auto CREATE. */
        private double vectorMergeMinScore = 0.92;

        private Executor executor = new Executor();

        @Data
        public static class Executor {
            private int corePoolSize = 4;
            private int maxPoolSize = 4;
            private int queueCapacity = 200;
        }
    }
}
