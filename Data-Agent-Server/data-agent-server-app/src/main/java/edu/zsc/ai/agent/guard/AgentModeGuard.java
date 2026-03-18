package edu.zsc.ai.agent.guard;

import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.error.ToolErrorCode;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.AgentRequestContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Guard utility that prevents execution-class tools from running in Plan mode.
 */
public final class AgentModeGuard {

    private AgentModeGuard() {
    }

    /**
     * Throws a typed tool exception if the current mode is PLAN.
     * Call at the top of any tool method that must be blocked in Plan mode.
     */
    public static void assertNotPlanMode(ToolNameEnum tool) {
        String mode = AgentRequestContext.getAgentMode();
        if (StringUtils.equalsIgnoreCase(AgentModeEnum.PLAN.getCode(), mode)) {
            throw new AgentToolExecuteException(
                    tool,
                    ToolErrorCode.PLAN_MODE_DISABLED,
                    tool.getToolName() + " is disabled in Plan mode — execution tools cannot run during planning. "
                            + "Keep the SQL in the planning response instead of executing it.",
                    "Tool blocked in plan mode",
                    "agentMode=" + mode,
                    false
            );
        }
    }
}
