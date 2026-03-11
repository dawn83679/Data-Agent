package edu.zsc.ai.agent.tool.plan;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
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
                    "Calling this tool delivers your plan to the user and greatly improves alignment — ",
                    "they can approve or adjust before any execution. Title + steps (order, description, SQL, objectName). Only in Plan mode.",
                    "",
                    "When to Use: when analysis is complete and you have a clear, step-by-step plan with production-ready SQL.",
                    "When NOT to Use: in Agent mode — this tool is only exposed in Plan mode.",
                    "Relation: pair with enterPlanMode; after exploration and analysis call this with all steps needed to achieve the goal."
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
