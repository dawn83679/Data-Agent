package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.Map;

import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

public abstract class AbstractSystemPromptHandler implements SystemPromptHandler {

    @Override
    public boolean support(PromptHandleRequest<SystemPromptAssemblyContext, SystemPromptSection> input) {
        return targetSection() == input.section();
    }

    @Override
    public PromptSectionResult<SystemPromptSection> handle(PromptHandleRequest<SystemPromptAssemblyContext, SystemPromptSection> input) {
        SystemPromptAssemblyContext context = input.context();
        return new PromptSectionResult<>(targetSection(), buildContent(context), true, buildMetadata(context));
    }

    @Override
    public int order() {
        return targetSection().ordinal();
    }

    protected Map<String, Object> buildMetadata(SystemPromptAssemblyContext context) {
        return Map.of();
    }

    protected abstract SystemPromptSection targetSection();

    protected abstract String buildContent(SystemPromptAssemblyContext context);
}
