package edu.zsc.ai.agent.tool.plan;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.plan.model.PlanStep;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

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
                    "Value: delivers the final structured plan payload to the user at the end of a planning flow.",
                    "Use When: call only when planning is complete and the plan is specific enough to execute without new design work.",
                    "After Success: wait for the user's decision before executing the plan or changing its scope.",
                    "After Failure: keep refining the plan in planning mode. Do not switch back to execution with an incomplete plan.",
                    "Wait For User: once the plan is presented, do not execute or broaden the scope until the user responds.",
                    "Result Consumption: the tool arguments carry the plan title and ordered steps that the frontend presents to the user.",
                    "Relation: this closes a plan flow started by enterPlanMode."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String exitPlanMode(
            @P("Plan title / summary") String title,
            @P("List of planned steps, each with order, description, SQL, and objectName") List<PlanStep> steps) {

        int stepCount = CollectionUtils.size(steps);
        log.info("[Tool] exitPlanMode, title='{}', steps={}", title, stepCount);

        // Minimal result for memory — full plan data is in the tool call arguments
        return ToolMessageSupport.sentence(
                "Plan '" + title + "' was presented to the user with " + stepCount + " step(s).",
                "Wait for the user's decision before executing the plan or changing its scope."
        );
    }
}
