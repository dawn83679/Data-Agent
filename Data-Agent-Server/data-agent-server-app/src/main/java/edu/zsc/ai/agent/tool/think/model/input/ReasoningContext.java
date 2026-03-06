package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReasoningContext {

    @Valid
    @JsonPropertyDescription("Structured runtime state snapshot.")
    private ReasoningState state;

    @Size(max = 2000, message = "stateSummary must not exceed 2000 characters")
    @JsonPropertyDescription("Readable state summary in key-value text form; used as fallback.")
    private String stateSummary;

    @Size(max = 8, message = "candidates size must not exceed 8")
    @Valid
    @JsonPropertyDescription("Optional SQL candidate list for ranking/selection.")
    private List<CandidateInput> candidates;
}

