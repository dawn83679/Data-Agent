package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class FeedbackPreference {

    @JsonPropertyDescription("Preference hint for future generation/selection decisions.")
    private String preferenceHint;
}
