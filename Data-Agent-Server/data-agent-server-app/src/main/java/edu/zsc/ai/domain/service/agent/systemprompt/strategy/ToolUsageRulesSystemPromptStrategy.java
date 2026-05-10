package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
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
        if (context.getAvailableSkills() != null && !context.getAvailableSkills().isEmpty()) {
            appendSkillRules(builder);
        } else {
            appendNoSkillRules(builder);
        }
        builder.append("\n");
        appendToolContractRules(builder, context.getAgentMode() == AgentModeEnum.PLAN);
        return builder.toString();
    }

    private void appendSkillRules(StringBuilder builder) {
        builder.append("- 可用技能列表描述本轮可用的可选技能；当某个已列出的技能能明显帮助当前任务时，可以调用 activateSkill。\n");
        builder.append("- activateSkill 成功后直接应用加载到的技能指导，不需要在最终答复里叙述激活过程。\n");
        builder.append("- 内部工具名通常不要写进最终用户答复，除非用户明确询问或这些信息有助于解释边界。\n");
    }

    private void appendNoSkillRules(StringBuilder builder) {
        builder.append("- 本轮没有可选技能可激活。\n");
        builder.append("- 内部工具名通常不要写进最终用户答复，除非用户明确询问或这些信息有助于解释边界。\n");
    }

    private void appendToolContractRules(StringBuilder builder, boolean planMode) {
        builder.append("- 工具的用途、参数、前置条件、结果语义和后续边界以工具自身描述、参数结构、运行时权限和工具返回消息为准。\n");
        builder.append("- 调用工具前确认当前范围和证据足以满足该工具前置条件；工具失败、空结果、需要确认或部分成功时，按工具返回状态继续，不要伪造结果。\n");
        if (planMode) {
            builder.append("- 计划模式下不要执行 SQL 或写操作；只把 SQL、风险、依赖和确认点组织成计划。\n");
        }
    }
}
