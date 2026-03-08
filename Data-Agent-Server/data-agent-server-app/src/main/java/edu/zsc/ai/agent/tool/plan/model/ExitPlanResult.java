package edu.zsc.ai.agent.tool.plan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured result of exitPlanMode tool, rendered as a plan card on the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExitPlanResult {

    private String title;
    private List<PlanStep> steps;
}
