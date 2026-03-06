package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.zsc.ai.agent.tool.think.model.enums.AmbiguityLevel;
import lombok.Data;

@Data
public class ReasoningState {

    @JsonPropertyDescription("Whether source/table scope has been resolved.")
    private Boolean sourceResolved;

    @JsonPropertyDescription("Whether schema metadata needed for SQL generation is ready.")
    private Boolean schemaReady;

    @JsonPropertyDescription("Whether current request implies write/side-effect SQL.")
    private Boolean writeOperation;

    @JsonPropertyDescription("Whether write operation has user confirmation.")
    private Boolean writeConfirmed;

    @JsonPropertyDescription("Whether current reasoning requires asking user a question.")
    private Boolean needUserQuestion;

    @JsonPropertyDescription("Whether last execution/validation produced an error.")
    private Boolean hasExecutionError;

    @JsonPropertyDescription("Overall readiness/confidence score in range [0,1].")
    private Double confidence;

    @JsonPropertyDescription("Retries already consumed in correction loop.")
    private Integer retriesUsed;

    @JsonPropertyDescription("Maximum retry budget allowed for this request.")
    private Integer retryBudget;

    @JsonPropertyDescription("Current number of SQL candidates available.")
    private Integer candidateCount;

    @JsonPropertyDescription("Current selected candidate ID, if any.")
    private String selectedCandidateId;

    @JsonPropertyDescription("Estimated ambiguity level of current user intent/schema mapping.")
    private AmbiguityLevel ambiguityLevel;
}
