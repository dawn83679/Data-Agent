package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.handler.AbstractHandlerChain;

@Component
public class MemoryRecallHandlerChain extends AbstractHandlerChain<
        MemoryRecallQuery,
        List<MemoryRecallItem>,
        MemoryRecallHandler> {

    public MemoryRecallHandlerChain(List<MemoryRecallHandler> handlers) {
        super(handlers);
    }
}
