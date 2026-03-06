package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.zsc.ai.agent.tool.think.model.enums.ThinkingStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReasoningMeta {

    @NotBlank(message = "goal must not be blank")
    @JsonPropertyDescription("Final user goal of the current NL2SQL request.")
    private String goal;

    @NotNull(message = "stage must not be null")
    @JsonPropertyDescription("Current reasoning stage in the state machine.")
    private ThinkingStage stage;
}

