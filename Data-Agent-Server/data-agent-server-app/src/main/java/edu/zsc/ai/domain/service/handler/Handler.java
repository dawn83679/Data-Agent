package edu.zsc.ai.domain.service.handler;

public interface Handler<I, O> {

    boolean support(I input);

    O handle(I input);
}
