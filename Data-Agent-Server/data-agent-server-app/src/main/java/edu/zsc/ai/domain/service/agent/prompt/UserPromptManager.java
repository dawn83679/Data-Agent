package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.zsc.ai.config.ai.PromptConfig;

@Component
public class UserPromptManager {

    static final String TEMPLATE_PATH = "prompt/user-prompt-template.md";

    private final UserPromptHandlerChain handlerChain;
    private final String template;

    @Autowired
    public UserPromptManager(UserPromptHandlerChain handlerChain) {
        this(handlerChain, PromptConfig.loadClassPathResource(TEMPLATE_PATH));
    }

    UserPromptManager(UserPromptHandlerChain handlerChain, String template) {
        this.handlerChain = handlerChain;
        this.template = template;
    }

    public PromptRenderResult<UserPromptSection> render(UserPromptAssemblyContext context) {
        Map<UserPromptSection, PromptSectionResult<UserPromptSection>> sections = handlerChain.renderSections(context);
        String rendered = template;
        for (UserPromptSection section : UserPromptSection.renderOrder()) {
            PromptSectionResult<UserPromptSection> result = sections.get(section);
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
                "Rendered runtime prompt sections: " + joiner);
    }
}
