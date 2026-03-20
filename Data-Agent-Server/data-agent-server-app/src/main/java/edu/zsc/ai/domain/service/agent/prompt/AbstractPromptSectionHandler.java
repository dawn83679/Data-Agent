package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Map;

import edu.zsc.ai.domain.service.handler.AbstractHandler;

public abstract class AbstractPromptSectionHandler<C, S>
        extends AbstractHandler<PromptHandleRequest<C, S>, PromptSectionResult<S>>
        implements PromptHandler<PromptHandleRequest<C, S>, PromptSectionResult<S>> {

    @Override
    public boolean support(PromptHandleRequest<C, S> input) {
        return targetSection() == input.section();
    }

    @Override
    protected final PromptSectionResult<S> doHandle(PromptHandleRequest<C, S> input) {
        C context = input.context();
        return new PromptSectionResult<>(targetSection(), buildContent(context), true, buildMetadata(context));
    }

    protected Map<String, Object> buildMetadata(C context) {
        return Map.of();
    }

    protected abstract S targetSection();

    protected abstract String buildContent(C context);
}
