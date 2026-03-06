package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReasoningNarrative {

    @JsonPropertyDescription("Current failure mode that blocks progress.")
    private String problem;

    @JsonPropertyDescription("Why this reasoning pattern is triggered at this moment.")
    private String trigger;

    @Size(max = 2000, message = "decompose must not exceed 2000 characters")
    @JsonPropertyDescription("Task decomposition and dependency plan.")
    private String decompose;

    @Size(max = 2000, message = "generate must not exceed 2000 characters")
    @JsonPropertyDescription("Candidate generation strategy.")
    private String generate;

    @Size(max = 2000, message = "select must not exceed 2000 characters")
    @JsonPropertyDescription("Candidate selection strategy.")
    private String select;

    @Size(max = 2000, message = "correct must not exceed 2000 characters")
    @JsonPropertyDescription("Self-correction and retry strategy.")
    private String correct;

    @Size(max = 2000, message = "memory must not exceed 2000 characters")
    @JsonPropertyDescription("Feedback-to-memory accumulation strategy.")
    private String memory;

    @Size(max = 2000, message = "fallback must not exceed 2000 characters")
    @JsonPropertyDescription("Abstain and downgrade strategy.")
    private String fallback;
}

