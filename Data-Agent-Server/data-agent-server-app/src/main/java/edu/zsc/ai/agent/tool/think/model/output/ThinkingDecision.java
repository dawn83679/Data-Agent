package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.zsc.ai.agent.tool.think.model.enums.ThinkingStage;
import lombok.Data;

import java.util.List;

@Data
public class ThinkingDecision {

    @JsonPropertyDescription("Next stage suggested by the decision engine.")
    private ThinkingStage nextStage;

    @JsonPropertyDescription("Next action identifier for orchestrator.")
    private String nextAction;

    @JsonPropertyDescription("Action payload with detailed execution hints.")
    private ActionPayload actionPayload;

    @JsonPropertyDescription("Candidate selection/generation policy for next step.")
    private CandidatePolicy candidatePolicy;

    @JsonPropertyDescription("Self-correction policy computed for current state.")
    private SelfCorrectionPolicy selfCorrection;

    @JsonPropertyDescription("Fallback policy when confidence/safety is insufficient.")
    private FallbackPolicy fallbackPolicy;

    @JsonPropertyDescription("Memory updates that should be persisted after this step.")
    private List<MemoryUpdate> memoryUpdates;

    @JsonPropertyDescription("Human-readable trace of decision reasoning.")
    private List<String> decisionTrace;
}
