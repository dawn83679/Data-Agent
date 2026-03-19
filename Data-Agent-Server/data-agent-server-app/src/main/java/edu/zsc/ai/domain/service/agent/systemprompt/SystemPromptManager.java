package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.Map;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.PromptRenderResult;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

@Component
public class SystemPromptManager {

    private final SystemPromptHandlerChain handlerChain;

    public SystemPromptManager(SystemPromptHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }

    public PromptRenderResult<SystemPromptSection> render(SystemPromptAssemblyContext context) {
        String template = PromptConfig.loadClassPathResource(context.getPromptEnum().getSystemPromptResource());
        Map<SystemPromptSection, PromptSectionResult<SystemPromptSection>> sections = handlerChain.renderSections(context);
        String rendered = template;
        for (SystemPromptSection section : SystemPromptSection.renderOrder()) {
            PromptSectionResult<SystemPromptSection> result = sections.get(section);
            rendered = rendered.replace(section.placeholder(), result == null ? "" : result.content());
        }

        int estimatedTokens = Math.max(1, rendered.length() / 4);
        StringJoiner joiner = new StringJoiner(", ");
        sections.forEach((section, result) -> {
            if (result != null && result.rendered()) {
                joiner.add(section.name());
            }
        });
        return new PromptRenderResult<>(rendered, sections, estimatedTokens,
                "Rendered system prompt sections: " + joiner);
    }
}
