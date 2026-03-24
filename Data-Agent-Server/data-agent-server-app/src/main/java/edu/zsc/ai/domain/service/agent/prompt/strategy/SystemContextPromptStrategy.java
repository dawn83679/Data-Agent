package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
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
        return UserPromptBlockSupport.renderBlock(
                context,
                "当前运行时环境：",
                "Current runtime environment:",
                List.of(
                        "today: " + (context.getCurrentDate() == null ? "" : context.getCurrentDate().toString()),
                        "timezone: " + StringUtils.defaultString(context.getTimezone())
                ));
    }
}
