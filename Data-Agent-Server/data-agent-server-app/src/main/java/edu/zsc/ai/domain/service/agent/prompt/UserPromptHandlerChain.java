package edu.zsc.ai.domain.service.agent.prompt;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UserPromptHandlerChain extends AbstractPromptHandlerChain<
        PromptHandleRequest<UserPromptAssemblyContext, UserPromptSection>,
        PromptSectionResult<UserPromptSection>> {

    public UserPromptHandlerChain(List<UserPromptHandler> handlers) {
        super(handlers);
    }

    public Map<UserPromptSection, PromptSectionResult<UserPromptSection>> renderSections(UserPromptAssemblyContext context) {
        Map<UserPromptSection, PromptSectionResult<UserPromptSection>> output = new EnumMap<>(UserPromptSection.class);
        for (UserPromptSection section : UserPromptSection.renderOrder()) {
            PromptHandleRequest<UserPromptAssemblyContext, UserPromptSection> input = new PromptHandleRequest<>(context, section);
            PromptSectionResult<UserPromptSection> result = handle(input, () -> new PromptSectionResult<>(section, "", false, Map.of()));
            output.put(section, result);
        }
        return output;
    }
}
