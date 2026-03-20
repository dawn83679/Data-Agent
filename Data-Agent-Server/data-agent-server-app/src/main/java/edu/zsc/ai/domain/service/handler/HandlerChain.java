package edu.zsc.ai.domain.service.handler;

public interface HandlerChain<I, O> {

    O handle(I input);
}
