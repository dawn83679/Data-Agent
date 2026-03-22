package edu.zsc.ai.domain.service.agent.prompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

@Component
public class SystemContextPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.SYSTEM_CONTEXT;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append("today: ")
                .append(PromptTextUtil.escape(context.getCurrentDate() == null ? "" : context.getCurrentDate().toString()))
                .append('\n');
        builder.append("timezone: ").append(PromptTextUtil.escape(context.getTimezone())).append('\n');
        builder.append("language: ").append(PromptTextUtil.escape(context.getLanguage())).append('\n');
        builder.append("agent_mode: ").append(PromptTextUtil.escape(context.getAgentMode())).append('\n');
        builder.append("model_name: ").append(PromptTextUtil.escape(context.getModelName()));
        return builder.toString();
    }
}
