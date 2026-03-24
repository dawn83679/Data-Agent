package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

@Component
public class UserQuestionPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.TASK;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        return UserPromptBlockSupport.renderBlock(
                context,
                "当前任务：",
                "Current task:",
                List.of(context.getUserMessage()));
    }
}
