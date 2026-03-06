package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class StateEntry {

    @JsonPropertyDescription("State key parsed from reasoning state snapshot.")
    private String key;

    @JsonPropertyDescription("State value rendered as readable text.")
    private String value;
}
