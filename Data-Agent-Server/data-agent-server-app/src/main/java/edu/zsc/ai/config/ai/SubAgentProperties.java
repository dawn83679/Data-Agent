package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for SubAgents.
 * Explorer and Planner have independent timeout settings.
 * Explorer dispatch additionally exposes bounded concurrency settings.
 *
 * Example application.yml:
 * <pre>
 * agent:
 *   sub-agent:
 *     explorer:
 *       timeout-seconds: 120
 *       dispatch:
 *         max-concurrency: 3
 *         queue-capacity: 9
 *     planner:
 *       timeout-seconds: 180
 *     max-explorer-loop: 3
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent.sub-agent")
public class SubAgentProperties {

    private ExplorerConfig explorer = new ExplorerConfig();
    private AgentConfig planner = new AgentConfig(180);
    private int maxExplorerLoop = 3;

    @Data
    public static class ExplorerConfig extends AgentConfig {
        private DispatchConfig dispatch = new DispatchConfig();

        public ExplorerConfig() {
            super(120);
        }
    }

    @Data
    public static class AgentConfig {
        private long timeoutSeconds;

        public AgentConfig() {
            this.timeoutSeconds = 120;
        }

        public AgentConfig(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    @Data
    public static class DispatchConfig {
        private int maxConcurrency = 3;
        private int queueCapacity = 9;
    }
}
