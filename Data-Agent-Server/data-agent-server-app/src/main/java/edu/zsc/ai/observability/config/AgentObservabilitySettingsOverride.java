package edu.zsc.ai.observability.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentObservabilitySettingsOverride {

    private Boolean enabled;
    private Boolean runtimeLogEnabled;
    private Boolean consoleLogEnabled;
    private Boolean sseEventLogEnabled;
    private Boolean modelEventLogEnabled;
    private Boolean toolEventLogEnabled;
    private Boolean includePrompt;
    private Boolean includeResponse;
    private Boolean includeTokenStream;
    private String runtimeLogDir;

    public boolean isEmpty() {
        return enabled == null
                && runtimeLogEnabled == null
                && consoleLogEnabled == null
                && sseEventLogEnabled == null
                && modelEventLogEnabled == null
                && toolEventLogEnabled == null
                && includePrompt == null
                && includeResponse == null
                && includeTokenStream == null
                && runtimeLogDir == null;
    }
}
