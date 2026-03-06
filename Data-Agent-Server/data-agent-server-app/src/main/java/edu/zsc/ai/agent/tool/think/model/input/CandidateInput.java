package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class CandidateInput {

    @JsonPropertyDescription("Stable identifier of this SQL candidate.")
    private String id;

    @JsonPropertyDescription("Candidate SQL text generated for the same user goal.")
    private String sql;

    @JsonPropertyDescription("Optional candidate confidence or ranking score.")
    private Double score;
}
