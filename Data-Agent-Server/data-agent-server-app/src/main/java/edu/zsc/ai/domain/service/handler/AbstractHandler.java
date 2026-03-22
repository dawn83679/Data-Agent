package edu.zsc.ai.domain.service.handler;

public abstract class AbstractHandler<I, O> implements Handler<I, O> {

    @Override
    public final O handle(I input) {
        return doHandle(input);
    }

    protected abstract O doHandle(I input);
}
