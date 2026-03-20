package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.domain.service.agent.systemprompt.AbstractSystemPromptHandler;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptSection;

@Component
public class ToolUsageRulesSystemPromptStrategy extends AbstractSystemPromptHandler {

    @Override
    protected SystemPromptSection targetSection() {
        return SystemPromptSection.TOOL_USAGE_RULES;
    }

    @Override
    protected String buildContent(SystemPromptAssemblyContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("- <skill_available> describes the optional skills supported in this session\n");
        builder.append("- activateSkill is available when one of those skills would meaningfully help with the current task\n");
        builder.append("- after activateSkill succeeds, you can apply the loaded guidance directly instead of narrating the activation itself\n");
        builder.append("- internal tool names usually stay out of the final user answer unless the user explicitly asks for them");
        if (context.getAgentType() == AgentTypeEnum.MAIN) {
            builder.append("\n- readMemory can help when prompt-injected memory is not enough and targeted durable context would clarify the task");
            builder.append("\n- writeMemory fits durable, reusable preferences, rules, facts, and validated patterns more than one-off task details");
            builder.append("\n- when the conversation reveals a stable preference, durable workflow rule, validated workspace fact, or reusable SQL pattern, you can consider activating the memory skill and writing memory during the turn");
            builder.append("\n- examples of memory-worthy signals: repeated output-format preference, workspace naming/governance constraint, or a verified reusable SQL aggregation pattern");
        }
        return builder.toString();
    }
}
