package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.handler.AbstractHandler;
<<<<<<< HEAD
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793

public abstract class AbstractScopeMemoryRecallHandler extends AbstractHandler<MemoryRecallQuery, List<MemoryRecallItem>>
        implements MemoryRecallHandler {

<<<<<<< HEAD
    private static final Logger runtimeLog = LoggerFactory.getLogger(MemoryRecallLogConstant.LOGGER_NAME);

    private final MemoryService memoryService;

    protected AbstractScopeMemoryRecallHandler(MemoryService memoryService) {
        this.memoryService = memoryService;
=======
    private final MemoryService memoryService;
    private final AgentLogService agentLogService;

    protected AbstractScopeMemoryRecallHandler(MemoryService memoryService, AgentLogService agentLogService) {
        this.memoryService = memoryService;
        this.agentLogService = agentLogService;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    @Override
    public boolean support(MemoryRecallQuery input) {
        return scope().matches(input.targetScope());
    }

    @Override
    protected List<MemoryRecallItem> doHandle(MemoryRecallQuery input) {
<<<<<<< HEAD
        runtimeLog.info("{} queryName={} planningReason={} targetScope={} matchedHandler={}",
                MemoryRecallLogConstant.EVENT_RECALL_QUERY_HANDLER_MATCH,
                input.queryName(),
                input.planningReason(),
                input.targetScope(),
                getClass().getSimpleName());
=======
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_QUERY_HANDLER_MATCH,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_QUERY_NAME, input.queryName(),
                        MemoryRecallLogConstant.FIELD_PLANNING_REASON, input.planningReason(),
                        MemoryRecallLogConstant.FIELD_TARGET_SCOPE, input.targetScope(),
                        MemoryRecallLogConstant.FIELD_MATCHED_HANDLER, getClass().getSimpleName()
                ));
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
                .queryStrategy(candidate.getQueryStrategy())
                .executionPath(candidate.getExecutionPath())
                .usedFallback(candidate.isUsedFallback())
                .conversationId(candidate.getConversationId())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }

    protected abstract MemoryScopeEnum scope();
}
