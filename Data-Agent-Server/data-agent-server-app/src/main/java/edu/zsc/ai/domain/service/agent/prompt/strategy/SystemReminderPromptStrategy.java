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
- %s defines what the user wants accomplished in this turn
- %s contains durable reusable rules recalled for this turn
- %s contains top-level natural-language preference lines and must be applied by default as a first-class instruction source for language and response format
- LANGUAGE_PREFERENCE and RESPONSE_FORMAT from %s override the incidental language or formatting inside %s
- only an explicit instruction in this turn can override those response preferences; incidental English, SQL, object names, or tool names do not count as a language or format switch
- before producing the final answer, re-check %s and make sure the final language, format, and visualization choices comply with those preferences
- %s contains a structured JSON array of database objects explicitly referenced by the user via @
- use connectionId, catalogName, schemaName, and objectName from %s as the default grounded scope before considering broader discovery
- when %s conflicts with durable memory, keep the task goal from %s while still honoring compatible response constraints from %s
- do not repeat these support blocks unless they are directly relevant to the answer"""
                .formatted(
                        UserPromptTagConstant.USER_QUESTION_OPEN,
                        UserPromptTagConstant.USER_MEMORY_OPEN,
                        UserPromptTagConstant.USER_PREFERENCES_OPEN,
                        UserPromptTagConstant.USER_PREFERENCES_OPEN,
                        UserPromptTagConstant.USER_QUESTION_OPEN,
                        UserPromptTagConstant.USER_PREFERENCES_OPEN,
                        UserPromptTagConstant.USER_MENTION_OPEN,
                        UserPromptTagConstant.USER_MENTION_OPEN,
                        UserPromptTagConstant.USER_QUESTION_OPEN,
                        UserPromptTagConstant.USER_QUESTION_OPEN,
                        UserPromptTagConstant.USER_PREFERENCES_OPEN
                );
    }
}
