package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class FeedbackSafety {

    @JsonPropertyDescription("Whether user has explicitly confirmed write execution.")
    private Boolean writeConfirmed;
}
