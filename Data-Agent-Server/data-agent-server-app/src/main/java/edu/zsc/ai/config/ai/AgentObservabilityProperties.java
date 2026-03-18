package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.observability")
public class AgentObservabilityProperties {

    private boolean enabled = false;
    private boolean runtimeLogEnabled = false;
    private boolean consoleLogEnabled = true;
    private boolean sseEventLogEnabled = true;
    private boolean modelEventLogEnabled = true;
    private boolean toolEventLogEnabled = true;
    private boolean includePrompt = false;
    private boolean includeResponse = true;
    private boolean includeTokenStream = true;
    private String runtimeLogDir = "~/.data-agent/logs/agent/runtime";
}
