package edu.zsc.ai.domain.service.ai.recall;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.observability.AgentLogService;

@Component
public class WorkspaceMemoryRecallHandler extends AbstractScopeMemoryRecallHandler {

    public WorkspaceMemoryRecallHandler(MemoryService memoryService, AgentLogService agentLogService) {
        super(memoryService, agentLogService);
    }

    @Override
    protected MemoryScopeEnum scope() {
        return MemoryScopeEnum.WORKSPACE;
    }
}
