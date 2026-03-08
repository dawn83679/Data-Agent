package edu.zsc.ai.agent.tool.plan;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.plan.model.PlanStep;
import lombok.extern.slf4j.Slf4j;

/**
 * Plan-mode exit tool: presents the structured execution plan to the user.
 * Uses IMMEDIATE return behavior to pause the agent stream and wait for user decision.
 * <p>
 * The full plan data is conveyed to the frontend via TOOL_CALL arguments (streamed via SSE).
 * The tool result stored in memory is kept minimal to save tokens on subsequent turns.
 */
@AgentTool
@Slf4j
public class ExitPlanModeTool {

    @Tool(
            value = {
                    "Delivers your finished plan to the user for review. A well-structured plan ",
                    "builds user confidence and ensures alignment before execution. The user can ",
                    "approve, modify, or reject the plan — this prevents wasted work and mistakes.",
                    "",
                    "Call this when your analysis is complete and you have a clear, step-by-step ",
                    "plan with production-ready SQL. Include all steps needed to achieve the goal."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String exitPlanMode(
            @P("Plan title / summary") String title,
            @P("List of planned steps, each with order, description, SQL, and objectName") List<PlanStep> steps) {

        int stepCount = steps != null ? steps.size() : 0;
        log.info("[Tool] exitPlanMode, title='{}', steps={}", title, stepCount);

        // Minimal result for memory — full plan data is in the tool call arguments
        return "Plan presented to user: " + title + " (" + stepCount + " steps).";
    }
}
