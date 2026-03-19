package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.domain.service.agent.systemprompt.AbstractSystemPromptHandler;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptSection;

@Component
public class AgentModeSystemPromptStrategy extends AbstractSystemPromptHandler {

    @Override
    protected SystemPromptSection targetSection() {
        return SystemPromptSection.AGENT_MODE;
    }

    @Override
    protected String buildContent(SystemPromptAssemblyContext context) {
        AgentTypeEnum agentType = context.getAgentType();
        AgentModeEnum agentMode = context.getAgentMode();
        if (agentType == AgentTypeEnum.MAIN && agentMode == AgentModeEnum.PLAN) {
            return "mode: plan\n- analyze, structure, and plan only\n- do not execute SQL or other side-effectful actions\n- still keep the answer actionable and implementation-ready";
        }
        if (agentType == AgentTypeEnum.MAIN) {
            return "mode: normal\n- you may orchestrate, inspect, plan, execute, and respond to the user\n- only use tools that are exposed to this agent in the current session";
        }
        if (agentType == AgentTypeEnum.PLANNER) {
            return "mode: planner\n- focus on SQL planning, validation, and optimization\n- do not behave like the user-facing orchestrator";
        }
        return "mode: explorer\n- focus on schema discovery and verified object understanding\n- do not speculate beyond inspected database metadata";
    }
}
