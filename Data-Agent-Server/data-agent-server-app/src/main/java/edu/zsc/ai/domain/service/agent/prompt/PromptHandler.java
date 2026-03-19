package edu.zsc.ai.domain.service.agent.prompt;

public interface PromptHandler<I, O> {

    boolean support(I input);

    O handle(I input);

    int order();
}
