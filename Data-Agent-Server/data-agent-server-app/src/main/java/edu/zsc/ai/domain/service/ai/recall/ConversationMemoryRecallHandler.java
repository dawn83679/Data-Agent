package edu.zsc.ai.domain.service.ai.recall;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;

@Component
public class ConversationMemoryRecallHandler extends AbstractScopeMemoryRecallHandler {

    @Override
    protected MemoryScopeEnum scope() {
        return MemoryScopeEnum.CONVERSATION;
    }

    @Override
    protected int scopeOrder() {
        return 0;
    }
}
