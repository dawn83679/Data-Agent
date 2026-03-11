package edu.zsc.ai.agent.tool.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent-mode escalation tool: switches from Agent mode to Plan mode for complex tasks.
 * Uses IMMEDIATE return behavior to stop the agent stream so the backend can
 * seamlessly chain a Plan-mode agent on the same SSE connection.
 * <p>
 * The tool result stored in memory is kept minimal to save tokens on subsequent turns.
 */
@AgentTool
@Slf4j
public class EnterPlanModeTool {

    @Tool(
            value = {
                    "Calling this tool greatly reduces risk on complex or write operations — you analyze ",
                    "and plan without executing, avoiding costly mistakes. Use exitPlanMode to deliver the plan.",
                    "",
                    "When to Use: write operations (DML/DDL), multi-step or multi-table tasks, vague goals, or when thinking suggests Plan mode.",
                    "When NOT to Use: for simple one-shot read-only queries with a clear target.",
                    "Relation: after entering, use getEnvironmentOverview/searchObjects/getObjectDetail and thinking; then exitPlanMode with title and steps. triggerSignal: CHECKLIST_RECOMMENDATION|MULTI_STEP_DISCOVERED|UNEXPECTED_COMPLEXITY|IRREVERSIBLE_OPERATION|MULTI_TABLE_WRITE."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String enterPlanMode(
            @P("Brief reason for planning") String reason,
            @P("Trigger: CHECKLIST_RECOMMENDATION | MULTI_STEP_DISCOVERED | " +
                    "UNEXPECTED_COMPLEXITY | IRREVERSIBLE_OPERATION | MULTI_TABLE_WRITE")
            String triggerSignal) {
        log.info("[Tool] enterPlanMode, reason='{}', trigger='{}'", reason, triggerSignal);
        return "Entering Plan mode [" + triggerSignal + "]: " + reason;
    }
}
