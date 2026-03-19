package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractPromptHandlerChain;
import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

@Component
public class SystemPromptHandlerChain extends AbstractPromptHandlerChain<
        PromptHandleRequest<SystemPromptAssemblyContext, SystemPromptSection>,
        PromptSectionResult<SystemPromptSection>> {

    public SystemPromptHandlerChain(List<SystemPromptHandler> handlers) {
        super(handlers);
    }

    public Map<SystemPromptSection, PromptSectionResult<SystemPromptSection>> renderSections(SystemPromptAssemblyContext context) {
        Map<SystemPromptSection, PromptSectionResult<SystemPromptSection>> output = new EnumMap<>(SystemPromptSection.class);
        for (SystemPromptSection section : SystemPromptSection.renderOrder()) {
            PromptHandleRequest<SystemPromptAssemblyContext, SystemPromptSection> input = new PromptHandleRequest<>(context, section);
            PromptSectionResult<SystemPromptSection> result = handle(input, () -> new PromptSectionResult<>(section, "", false, Map.of()));
            output.put(section, result);
        }
        return output;
    }
}
