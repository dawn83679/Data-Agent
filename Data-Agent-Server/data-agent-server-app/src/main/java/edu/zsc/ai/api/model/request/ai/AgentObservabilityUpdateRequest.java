package edu.zsc.ai.api.model.request.ai;

import lombok.Data;

@Data
public class AgentObservabilityUpdateRequest {

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
}
