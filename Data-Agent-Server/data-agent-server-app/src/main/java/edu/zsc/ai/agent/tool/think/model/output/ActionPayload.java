package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class ActionPayload {

    @JsonPropertyDescription("Reason for choosing the next action.")
    private String reason;

    @JsonPropertyDescription("Machine-readable instruction for orchestrator/tooling.")
    private String instruction;

    @JsonPropertyDescription("Question type when next action is ask-user.")
    private String questionType;

    @JsonPropertyDescription("Recommended chain/flow for follow-up handling.")
    private String recommendedChain;

    @JsonPropertyDescription("Candidate ID to use when action targets a selected SQL.")
    private String selectedCandidateId;

    @JsonPropertyDescription("Target candidate count when generating/reranking candidates.")
    private Integer targetCount;
}
