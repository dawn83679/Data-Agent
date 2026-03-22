package edu.zsc.ai.domain.service.ai.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.handler.AbstractHandler;

class MemoryRecallHandlerChainTest {

    @Test
    void handle_dispatchesToTheSingleMatchingHandler() {
        MemoryRecallHandlerChain chain = new MemoryRecallHandlerChain(List.of(
                new TestMemoryRecallHandler(MemoryScopeEnum.USER.getCode(), 1L),
                new TestMemoryRecallHandler(MemoryScopeEnum.CONVERSATION.getCode(), 2L)));

        List<MemoryRecallItem> items = chain.handle(query(MemoryScopeEnum.CONVERSATION.getCode()));

        assertEquals(1, items.size());
        assertEquals(2L, items.get(0).getId());
    }

    @Test
    void handle_rejectsMissingHandler() {
        MemoryRecallHandlerChain chain = new MemoryRecallHandlerChain(List.of(
                new TestMemoryRecallHandler(MemoryScopeEnum.USER.getCode(), 1L)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> chain.handle(query(MemoryScopeEnum.CONVERSATION.getCode())));

        assertEquals("No handler matched input: " + query(MemoryScopeEnum.CONVERSATION.getCode()), exception.getMessage());
    }

    @Test
    void handle_rejectsMultipleMatchingHandlers() {
        MemoryRecallHandlerChain chain = new MemoryRecallHandlerChain(List.of(
                new TestMemoryRecallHandler(MemoryScopeEnum.USER.getCode(), 1L),
                new TestMemoryRecallHandler(MemoryScopeEnum.USER.getCode(), 2L)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> chain.handle(query(MemoryScopeEnum.USER.getCode())));

        assertEquals("Multiple handlers matched input: " + query(MemoryScopeEnum.USER.getCode()), exception.getMessage());
    }

    private MemoryRecallQuery query(String scope) {
        return new MemoryRecallQuery("test", "test_reason", scope, 7L, "find memory", null, null, 0.2D,
                MemoryRecallMode.PROMPT, MemoryRecallQueryStrategy.HYBRID, 0);
    }

    private static final class TestMemoryRecallHandler extends AbstractHandler<MemoryRecallQuery, List<MemoryRecallItem>>
            implements MemoryRecallHandler {

        private final String scope;
        private final long id;

        private TestMemoryRecallHandler(String scope, long id) {
            this.scope = scope;
            this.id = id;
        }

        @Override
        public boolean support(MemoryRecallQuery input) {
            return scope.equals(input.targetScope());
        }

        @Override
        protected List<MemoryRecallItem> doHandle(MemoryRecallQuery input) {
            return List.of(MemoryRecallItem.builder().id(id).scope(scope).build());
        }
    }
}
