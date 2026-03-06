package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
public class ThinkingOutput {

    @JsonPropertyDescription("Structured reasoning block for current iteration.")
    private StructuredReasoning structuredReasoning;

    @JsonPropertyDescription("Next stage name for frontend rendering convenience.")
    private String nextStage;

    @JsonPropertyDescription("Next action identifier for frontend/orchestrator.")
    private String nextAction;

    @JsonPropertyDescription("Action payload generated for next action.")
    private ActionPayload actionPayload;

    @JsonPropertyDescription("Candidate policy for generation/selection.")
    private CandidatePolicy candidatePolicy;

    @JsonPropertyDescription("Self-correction policy details.")
    private SelfCorrectionPolicy selfCorrection;

    @JsonPropertyDescription("Fallback policy details.")
    private FallbackPolicy fallbackPolicy;

    @JsonPropertyDescription("Memory updates produced by this decision.")
    private List<MemoryUpdate> memoryUpdates;

    @JsonPropertyDescription("Decision trace for debugging and display.")
    private List<String> decisionTrace;

    @JsonPropertyDescription("Full typed decision object.")
    private ThinkingDecision decision;
}
