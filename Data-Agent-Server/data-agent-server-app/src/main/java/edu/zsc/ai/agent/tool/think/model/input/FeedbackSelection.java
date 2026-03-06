package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
public class FeedbackSelection {

    @JsonPropertyDescription("Candidate ID selected by user/system.")
    private String selectedCandidateId;

    @JsonPropertyDescription("Candidate IDs explicitly rejected in this iteration.")
    private List<String> rejectedCandidateIds;
}
