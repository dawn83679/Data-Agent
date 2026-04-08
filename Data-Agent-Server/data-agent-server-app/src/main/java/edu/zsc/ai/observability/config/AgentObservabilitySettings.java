package edu.zsc.ai.observability.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentObservabilitySettings {

    private boolean enabled;
    private boolean runtimeLogEnabled;
    private boolean consoleLogEnabled;
    private boolean sseEventLogEnabled;
    private boolean modelEventLogEnabled;
    private boolean toolEventLogEnabled;
    private boolean includeResponse;
    private boolean includeTokenStream;
    private String runtimeLogDir;
}
