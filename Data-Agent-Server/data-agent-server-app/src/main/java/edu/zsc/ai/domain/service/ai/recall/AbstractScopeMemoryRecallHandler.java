package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.domain.service.handler.AbstractHandler;
import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractScopeMemoryRecallHandler extends AbstractHandler<MemoryRecallQuery, List<MemoryRecallItem>>
        implements MemoryRecallHandler {

    private static final Logger runtimeLog = LoggerFactory.getLogger(MemoryRecallLogConstant.LOGGER_NAME);

    private final MemoryService memoryService;

    protected AbstractScopeMemoryRecallHandler(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public boolean support(MemoryRecallQuery input) {
        return scope().matches(input.targetScope());
    }

    @Override
    protected List<MemoryRecallItem> doHandle(MemoryRecallQuery input) {
        runtimeLog.info("{} queryName={} planningReason={} targetScope={} matchedHandler={}",
                MemoryRecallLogConstant.EVENT_RECALL_QUERY_HANDLER_MATCH,
                input.queryName(),
                input.planningReason(),
                input.targetScope(),
                getClass().getSimpleName());
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
