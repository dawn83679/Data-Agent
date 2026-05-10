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
            return "模式：计划\n- 只分析、梳理和制定计划\n- 不执行 SQL 或其他有副作用的动作\n- 计划仍要具体、可执行、可交接";
        }
        if (agentType == AgentTypeEnum.MAIN) {
            return "模式：普通执行\n- 可以编排、检查、计划、执行并回复用户\n- 只使用当前会话暴露给该 agent 的工具";
        }
        if (agentType == AgentTypeEnum.PLANNER) {
            return "模式：SQL 规划\n- 专注 SQL 规划、校验和优化\n- 不要表现成面向用户的主编排 agent";
        }
        if (agentType == AgentTypeEnum.MEMORY_WRITER) {
            return "模式：后台记忆写入\n- 你是内部后台记忆写入 agent，不面向用户\n- 结束前必须保持当前对话工作记忆最新\n- 只使用当前会话暴露给该 agent 的工具";
        }
        return "模式：schema 探索\n- 专注 schema 探索和已验证对象理解\n- 不要超出已检查的数据库元数据做猜测";
    }
}
