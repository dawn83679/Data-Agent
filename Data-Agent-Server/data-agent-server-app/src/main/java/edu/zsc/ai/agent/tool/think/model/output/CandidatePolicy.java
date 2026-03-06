package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class CandidatePolicy {

    @JsonPropertyDescription("Candidate strategy mode (single/multi/rerank/etc).")
    private String mode;

    @JsonPropertyDescription("Reason for current candidate policy choice.")
    private String reason;

    @JsonPropertyDescription("Observed candidate count in current context.")
    private Integer candidateCount;

    @JsonPropertyDescription("Maximum shortlist size for selection/validation.")
    private Integer shortlistMax;

    @JsonPropertyDescription("Policy-selected candidate ID, if available.")
    private String selectedCandidateId;
}
