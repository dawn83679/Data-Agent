package edu.zsc.ai.domain.service.agent.prompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

@Component
public class UserQuestionPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.USER_QUESTION;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        return PromptTextUtil.escape(context.getUserMessage());
    }
}
