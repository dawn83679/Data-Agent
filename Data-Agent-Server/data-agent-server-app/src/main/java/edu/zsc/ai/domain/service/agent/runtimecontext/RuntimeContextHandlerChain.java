package edu.zsc.ai.domain.service.agent.runtimecontext;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;
import edu.zsc.ai.domain.service.handler.AbstractHandlerChain;

@Component
public class RuntimeContextHandlerChain extends AbstractHandlerChain<
        PromptHandleRequest<RuntimeContextAssemblyContext, RuntimeContextSection>,
        PromptSectionResult<RuntimeContextSection>,
        RuntimeContextHandler> {

    public RuntimeContextHandlerChain(List<RuntimeContextHandler> handlers) {
        super(handlers);
    }

    public Map<RuntimeContextSection, PromptSectionResult<RuntimeContextSection>> renderSections(RuntimeContextAssemblyContext context) {
        Map<RuntimeContextSection, PromptSectionResult<RuntimeContextSection>> output = new EnumMap<>(RuntimeContextSection.class);
        for (RuntimeContextSection section : RuntimeContextSection.renderOrder()) {
            PromptHandleRequest<RuntimeContextAssemblyContext, RuntimeContextSection> input = new PromptHandleRequest<>(context, section);
            PromptSectionResult<RuntimeContextSection> result = handle(input);
            output.put(section, result);
        }
        return output;
    }
}
