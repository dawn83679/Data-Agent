package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import java.util.Locale;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
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
        if (context.getAvailableSkills() != null && !context.getAvailableSkills().isEmpty()) {
            builder.append("- <skill_available> describes the optional skills supported in this session\n");
            builder.append("- activateSkill is available when one of those listed skills would meaningfully help with the current task\n");
            builder.append("- after activateSkill succeeds, you can apply the loaded guidance directly instead of narrating the activation itself\n");
            builder.append("- internal tool names usually stay out of the final user answer unless the user explicitly asks for them");
        } else {
            builder.append("- no optional skills are available in this session\n");
            builder.append("- internal tool names usually stay out of the final user answer unless the user explicitly asks for them");
        }
        builder.append("\n");
        String language = context.getLanguage();
        boolean isZh = language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
        if (isZh) {
            builder.append("- 仅在当前轮次仍未明确连接、数据库、模式或对象范围，或用户明确询问可用连接时才调用 getConnections；当已有显式引用定位目标时，先在该范围内行动，不要首先调用它。\n");
        } else {
            builder.append("- Only call getConnections when the connection/catalog/schema/object scope is still undefined or the user explicitly asks for the available connections; rely on explicit references instead of calling it first when they already ground the scope.\n");
        }
        return builder.toString();
    }
}
