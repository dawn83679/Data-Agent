package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreflightChecklist {

    @JsonPropertyDescription("Brief summary of goal and current context state")
    private String summary;

    @JsonPropertyDescription("Actions that MUST be completed before executing SQL")
    private List<ChecklistItem> requiredBefore;

    @JsonPropertyDescription("Actions that are recommended but not strictly required")
    private List<ChecklistItem> recommended;

    @JsonPropertyDescription("Identified risks or concerns")
    private List<String> risks;

    @JsonPropertyDescription("Complexity assessment: simple, moderate, or complex")
    private String complexityAssessment;

    @JsonPropertyDescription("Whether Plan mode is recommended for this task")
    private boolean suggestPlanMode;

    @JsonPropertyDescription("Reason for Plan mode recommendation, if applicable")
    private String suggestPlanModeReason;
}
