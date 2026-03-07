package edu.zsc.ai.agent.tool.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
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
                    "[GOAL] Switch to Plan mode for thorough analysis before execution.",
                    "[WHEN] Complex multi-step DDL/DML, data migrations, schema redesigns, " +
                            "bulk operations affecting large datasets, or irreversible operations.",
                    "[WHEN_NOT] Simple SELECT, single INSERT/UPDATE/DELETE, creating one table, " +
                            "or user explicitly requests immediate execution."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String enterPlanMode(@P("Brief reason for planning") String reason) {
        log.info("[Tool] enterPlanMode, reason='{}'", reason);
        return "Entering Plan mode: " + reason;
    }
}
