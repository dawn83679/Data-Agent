package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractPromptHandlerChain<I, O> {

    private final List<PromptHandler<I, O>> orderedHandlers;

    protected AbstractPromptHandlerChain(List<? extends PromptHandler<I, O>> handlers) {
        this.orderedHandlers = handlers.stream()
                .map(handler -> (PromptHandler<I, O>) handler)
                .sorted(Comparator.comparingInt(PromptHandler::order))
                .toList();
    }

    protected O handle(I input, Supplier<O> defaultSupplier) {
        return orderedHandlers.stream()
                .filter(handler -> handler.support(input))
                .findFirst()
                .map(handler -> handler.handle(input))
                .orElseGet(defaultSupplier);
    }
}
