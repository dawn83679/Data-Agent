package edu.zsc.ai.domain.service.agent.prompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.UserPromptTagConstant;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

@Component
public class SystemReminderPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.SYSTEM_REMINDER;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        return """
- %s is the primary task of this turn
- %s is supportive background, not the task itself
- %s contains database objects explicitly referenced by the user via @
- when current user instruction conflicts with memory, follow the current user instruction
- do not repeat these support blocks unless they are directly relevant to the answer"""
                .formatted(
                        UserPromptTagConstant.USER_QUESTION_OPEN,
                        UserPromptTagConstant.USER_MEMORY_OPEN,
                        UserPromptTagConstant.USER_MENTION_OPEN
                );
    }
}
