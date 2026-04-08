package edu.zsc.ai.domain.service.ai.recall;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;

@Component
public class ConversationMemoryRecallHandler extends AbstractScopeMemoryRecallHandler {

    public ConversationMemoryRecallHandler(MemoryService memoryService) {
        super(memoryService);
    }

    @Override
    protected MemoryScopeEnum scope() {
        return MemoryScopeEnum.CONVERSATION;
    }
}
