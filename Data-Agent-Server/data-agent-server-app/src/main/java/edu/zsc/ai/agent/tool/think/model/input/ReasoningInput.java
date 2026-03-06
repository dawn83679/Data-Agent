package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReasoningInput {
    @Valid
    @NotNull(message = "meta must not be null")
    @JsonPropertyDescription("High-level goal and stage.")
    private ReasoningMeta meta;

    @Valid
    @JsonPropertyDescription("Narrative reasoning blocks for this step.")
    private ReasoningNarrative narrative;

    @Valid
    @JsonPropertyDescription("State snapshot and candidate context.")
    private ReasoningContext context;
}
