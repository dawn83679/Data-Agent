package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ThinkingRequest {

    @NotNull(message = "reasoning must not be null")
    @Valid
    @JsonPropertyDescription("Structured reasoning object.")
    private ReasoningInput reasoning;

    @Valid
    @JsonPropertyDescription("Optional feedback object for selection/correction/confirmation.")
    private FeedbackInput feedback;

    @JsonPropertyDescription("Whether another reasoning iteration is needed. Default true.")
    private Boolean nextThoughtNeeded;
}

