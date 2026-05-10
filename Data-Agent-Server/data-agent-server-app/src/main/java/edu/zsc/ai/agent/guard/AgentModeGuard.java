package edu.zsc.ai.agent.guard;

import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.error.ToolErrorCode;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.AgentRequestContext;
import org.apache.commons.lang3.StringUtils;

public final class AgentModeGuard {

    private AgentModeGuard() {
    }

    public static void assertNotPlanMode(ToolNameEnum tool) {
        String mode = AgentRequestContext.getAgentMode();
        if (StringUtils.equalsIgnoreCase(AgentModeEnum.PLAN.getCode(), mode)) {
            throw new AgentToolExecuteException(
                    tool,
                    ToolErrorCode.PLAN_MODE_DISABLED,
                    tool.getToolName() + " 在 Plan 模式下不可用，规划期间不能运行执行类工具。"
                            + "把 SQL 保留在计划答复中，不要执行它。",
                    "工具在 plan 模式下被阻止",
                    "agentMode=" + mode,
                    false
            );
        }
    }
}
