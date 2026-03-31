package edu.zsc.ai.domain.service.agent.runtimecontext;

import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.domain.service.agent.prompt.PromptRenderResult;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

@Component
public class RuntimeContextManager {

    static final String TEMPLATE_PATH = "prompt/runtime-context-template.md";

    private final RuntimeContextHandlerChain handlerChain;
    private final String template;

    @Autowired
    public RuntimeContextManager(RuntimeContextHandlerChain handlerChain) {
        this(handlerChain, PromptConfig.loadClassPathResource(TEMPLATE_PATH));
    }

    RuntimeContextManager(RuntimeContextHandlerChain handlerChain, String template) {
        this.handlerChain = handlerChain;
        this.template = template;
    }

    public PromptRenderResult<RuntimeContextSection> render(RuntimeContextAssemblyContext context) {
        Map<RuntimeContextSection, PromptSectionResult<RuntimeContextSection>> sections = handlerChain.renderSections(context);
        String rendered = template;
        for (RuntimeContextSection section : RuntimeContextSection.renderOrder()) {
            PromptSectionResult<RuntimeContextSection> result = sections.get(section);
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
                "Rendered runtime context sections: " + joiner);
    }
}
