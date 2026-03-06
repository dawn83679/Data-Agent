package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class FallbackPolicy {

    @JsonPropertyDescription("Whether to abstain from unsafe/low-confidence execution.")
    private Boolean abstain;

    @JsonPropertyDescription("Fallback path name (ask_user, regenerate, stop, etc).")
    private String path;

    @JsonPropertyDescription("Reason that triggers fallback/abstain.")
    private String reason;
}
