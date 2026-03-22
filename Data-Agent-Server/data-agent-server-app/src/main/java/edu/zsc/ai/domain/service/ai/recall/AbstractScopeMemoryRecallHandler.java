package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.handler.AbstractHandler;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;

public abstract class AbstractScopeMemoryRecallHandler extends AbstractHandler<MemoryRecallQuery, List<MemoryRecallItem>>
        implements MemoryRecallHandler {

    private final MemoryService memoryService;
    private final AgentLogService agentLogService;

    protected AbstractScopeMemoryRecallHandler(MemoryService memoryService, AgentLogService agentLogService) {
        this.memoryService = memoryService;
        this.agentLogService = agentLogService;
    }

    @Override
    public boolean support(MemoryRecallQuery input) {
        return scope().matches(input.targetScope());
    }

    @Override
    protected List<MemoryRecallItem> doHandle(MemoryRecallQuery input) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_QUERY_HANDLER_MATCH,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_QUERY_NAME, input.queryName(),
                        MemoryRecallLogConstant.FIELD_PLANNING_REASON, input.planningReason(),
                        MemoryRecallLogConstant.FIELD_TARGET_SCOPE, input.targetScope(),
                        MemoryRecallLogConstant.FIELD_MATCHED_HANDLER, getClass().getSimpleName()
                ));
        return memoryService.recallAccessibleMemories(input)
                .stream()
                .map(this::toRecallItem)
                .toList();
    }

    protected MemoryRecallItem toRecallItem(MemorySearchResult candidate) {
        return MemoryRecallItem.builder()
                .id(candidate.getId())
                .scope(candidate.getScope())
                .memoryType(candidate.getMemoryType())
                .subType(candidate.getSubType())
                .title(candidate.getTitle())
                .content(candidate.getContent())
                .reason(candidate.getReason())
                .sourceType(candidate.getSourceType())
                .score(candidate.getScore())
                .conversationId(candidate.getConversationId())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }

    protected abstract MemoryScopeEnum scope();
}
