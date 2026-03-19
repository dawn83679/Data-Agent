package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.SkillPromptTagConstant;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.systemprompt.AbstractSystemPromptHandler;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptSection;

@Component
public class SkillAvailableSystemPromptStrategy extends AbstractSystemPromptHandler {

    @Override
    protected SystemPromptSection targetSection() {
        return SystemPromptSection.SKILL_AVAILABLE;
    }

    @Override
    protected String buildContent(SystemPromptAssemblyContext context) {
        if (context.getAvailableSkills() == null || context.getAvailableSkills().isEmpty()) {
            return PromptConstant.NONE;
        }
        StringBuilder builder = new StringBuilder();
        for (SkillEnum skill : context.getAvailableSkills()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(SkillPromptTagConstant.open(skill.getSkillName()))
                    .append(PromptTextUtil.escape(skill.getDescription()))
                    .append(SkillPromptTagConstant.close(skill.getSkillName()));
        }
        return builder.toString();
    }
}
