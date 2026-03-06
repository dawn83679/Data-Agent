package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningState;
import lombok.Data;

import java.util.List;

@Data
public class StructuredReasoning {

    @JsonPropertyDescription("Problem: core failure mode currently blocking progress.")
    private String problem;

    @JsonPropertyDescription("Trigger: why this reasoning pattern is activated now.")
    private String trigger;

    @JsonPropertyDescription("Structured runtime state used for decision making.")
    private ReasoningState state;

    @JsonPropertyDescription("Readable state summary when structured state is incomplete.")
    private String stateSummary;

    @JsonPropertyDescription("Parsed state entries for display.")
    private List<StateEntry> stateParsed;

    @JsonPropertyDescription("Decompose: task split and stage dependency plan.")
    private String decompose;

    @JsonPropertyDescription("Generate: candidate generation strategy.")
    private String generate;

    @JsonPropertyDescription("Select: candidate selection strategy.")
    private String select;

    @JsonPropertyDescription("Correct: self-correction loop strategy.")
    private String correct;

    @JsonPropertyDescription("Memory: feedback and memory accumulation strategy.")
    private String memory;

    @JsonPropertyDescription("Fallback: abstain and downgrade strategy.")
    private String fallback;

    @JsonPropertyDescription("Optional feedback object used in current reasoning step.")
    private FeedbackInput feedback;

    @JsonPropertyDescription("Missing reasoning dimensions that should be filled next.")
    private List<String> missingDimensions;

    @JsonPropertyDescription("Readiness score summarizing stage completion confidence.")
    private Double readinessScore;
}
