package edu.zsc.ai.domain.service.ai.recall;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;

@Component
public class UserMemoryRecallHandler extends AbstractScopeMemoryRecallHandler {

    public UserMemoryRecallHandler(MemoryService memoryService) {
        super(memoryService);
    }

    @Override
    protected MemoryScopeEnum scope() {
        return MemoryScopeEnum.USER;
    }
}
