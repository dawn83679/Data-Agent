package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class MemoryUpdate {

    @JsonPropertyDescription("Memory update type (preference, mapping, join_path, etc).")
    private String type;

    @JsonPropertyDescription("Memory content to persist.")
    private String content;

    @JsonPropertyDescription("Reason why this memory update is created.")
    private String reason;
}
