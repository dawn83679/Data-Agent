package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class FeedbackInput {

    @Valid
    @JsonPropertyDescription("Selection feedback such as chosen/rejected candidate IDs.")
    private FeedbackSelection selection;

    @Valid
    @JsonPropertyDescription("Correction feedback containing revised SQL.")
    private FeedbackCorrection correction;

    @Valid
    @JsonPropertyDescription("Safety confirmation feedback for write operations.")
    private FeedbackSafety safety;

    @Valid
    @JsonPropertyDescription("User preference feedback for future reasoning/memory.")
    private FeedbackPreference preference;
}
