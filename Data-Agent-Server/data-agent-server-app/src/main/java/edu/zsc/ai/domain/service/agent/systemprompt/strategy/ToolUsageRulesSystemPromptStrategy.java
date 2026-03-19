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
        builder.append("- only call activateSkill when one of the skills listed in <skill_available> is clearly needed\n");
        builder.append("- after activateSkill succeeds, directly apply the loaded rules instead of narrating the activation itself\n");
        builder.append("- do not invent or call unlisted skills\n");
        builder.append("- do not expose internal tool names in the final user answer unless the user explicitly asks for them");
        if (context.getAgentType() == AgentTypeEnum.MAIN) {
            builder.append("\n- use readMemory only when prompt-injected memory is insufficient and you need targeted durable context; do not call it mechanically every turn");
            builder.append("\n- use writeMemory only for durable, reusable memory; do not write one-off task details");
            builder.append("\n- when the conversation reveals a stable preference, durable workflow rule, validated workspace fact, or reusable SQL pattern, consider activating the memory skill and writing memory during the turn");
            builder.append("\n- examples of memory-worthy signals: repeated output-format preference, workspace naming/governance constraint, or a verified reusable SQL aggregation pattern");
        }
        return builder.toString();
    }
}
