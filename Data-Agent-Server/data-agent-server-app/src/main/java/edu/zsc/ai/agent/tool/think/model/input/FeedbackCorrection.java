package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class FeedbackCorrection {

    @JsonPropertyDescription("Corrected SQL provided by user or post-check stage.")
    private String correctedSql;
}
