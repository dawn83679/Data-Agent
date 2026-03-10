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

    @JsonPropertyDescription("Action to take, e.g. 'Survey the environment using currently visible discovery tools'")
    private String action;

    @JsonPropertyDescription("Suggested tool category to use, based on the tools visible in the current session")
    private String toolToCall;

    @JsonPropertyDescription("Why this step is needed")
    private String reason;
}
