package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class SelfCorrectionPolicy {

    @JsonPropertyDescription("Whether self-correction loop is enabled.")
    private Boolean enabled;

    @JsonPropertyDescription("Trigger condition text for entering correction.")
    private String trigger;

    @JsonPropertyDescription("Maximum retries allowed in this correction loop.")
    private Integer maxRetries;

    @JsonPropertyDescription("Retries already used.")
    private Integer retriesUsed;

    @JsonPropertyDescription("Retries remaining.")
    private Integer remainingRetries;

    @JsonPropertyDescription("Whether to retry immediately in current turn.")
    private Boolean retryNow;
}
