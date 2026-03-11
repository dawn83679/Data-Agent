package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @JsonPropertyDescription("Action to take, e.g. 'Call getEnvironmentOverview to resolve connectionId'")
    private String action;

    @JsonPropertyDescription("Tool to call, e.g. 'getEnvironmentOverview'")
    private String toolToCall;

    @JsonPropertyDescription("Why this step is needed")
    private String reason;
}
