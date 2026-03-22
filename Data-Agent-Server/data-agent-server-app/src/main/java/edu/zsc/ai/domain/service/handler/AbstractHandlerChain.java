package edu.zsc.ai.domain.service.handler;

import java.util.List;

public abstract class AbstractHandlerChain<I, O, H extends Handler<I, O>> implements HandlerChain<I, O> {

    private final List<H> handlers;

    protected AbstractHandlerChain(List<? extends H> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public O handle(I input) {
        H matchedHandler = null;
        for (H handler : handlers) {
            if (!handler.support(input)) {
                continue;
            }
            if (matchedHandler != null) {
                throw new IllegalStateException("Multiple handlers matched input: " + input);
            }
            matchedHandler = handler;
        }
        if (matchedHandler == null) {
            throw new IllegalStateException("No handler matched input: " + input);
        }
        return matchedHandler.handle(input);
    }
}
