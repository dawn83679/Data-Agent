package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Map;

public abstract class AbstractUserPromptHandler implements UserPromptHandler {

    @Override
    public boolean support(PromptHandleRequest<UserPromptAssemblyContext, UserPromptSection> input) {
        return targetSection() == input.section();
    }

    @Override
    public PromptSectionResult<UserPromptSection> handle(PromptHandleRequest<UserPromptAssemblyContext, UserPromptSection> input) {
        UserPromptAssemblyContext context = input.context();
        return new PromptSectionResult<>(targetSection(), buildContent(context), true, buildMetadata(context));
    }

    @Override
    public int order() {
        return targetSection().ordinal();
    }

    protected Map<String, Object> buildMetadata(UserPromptAssemblyContext context) {
        return Map.of();
    }

    protected abstract UserPromptSection targetSection();

    protected abstract String buildContent(UserPromptAssemblyContext context);
}
