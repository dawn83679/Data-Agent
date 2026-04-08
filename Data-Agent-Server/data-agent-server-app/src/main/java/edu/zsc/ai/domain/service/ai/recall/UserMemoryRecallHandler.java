package edu.zsc.ai.domain.service.ai.recall;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;
<<<<<<< HEAD
=======
import edu.zsc.ai.observability.AgentLogService;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793

@Component
public class UserMemoryRecallHandler extends AbstractScopeMemoryRecallHandler {

<<<<<<< HEAD
    public UserMemoryRecallHandler(MemoryService memoryService) {
        super(memoryService);
=======
    public UserMemoryRecallHandler(MemoryService memoryService, AgentLogService agentLogService) {
        super(memoryService, agentLogService);
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    @Override
    protected MemoryScopeEnum scope() {
        return MemoryScopeEnum.USER;
    }
}
