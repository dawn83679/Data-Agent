package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

public abstract class AbstractScopeMemoryRecallHandler implements MemoryRecallHandler {

    @Override
    public boolean support(MemoryRecallContext context) {
        return StringUtils.isBlank(context.getScope()) || scope().matches(context.getScope());
    }

    @Override
    public List<MemoryRecallItem> handle(MemoryRecallContext context, List<MemorySearchResult> candidates) {
        return candidates.stream()
                .filter(candidate -> scope().matches(candidate.getScope()))
                .map(this::toRecallItem)
                .toList();
    }

    @Override
    public int order() {
        return scopeOrder();
    }

    protected MemoryRecallItem toRecallItem(MemorySearchResult candidate) {
        return MemoryRecallItem.builder()
                .id(candidate.getId())
                .scope(candidate.getScope())
                .workspaceLevel(candidate.getWorkspaceLevel())
                .workspaceContextKey(candidate.getWorkspaceContextKey())
                .memoryType(candidate.getMemoryType())
                .subType(candidate.getSubType())
                .title(candidate.getTitle())
                .content(candidate.getContent())
                .normalizedContentKey(candidate.getNormalizedContentKey())
                .reason(candidate.getReason())
                .reviewState(candidate.getReviewState())
                .sourceType(candidate.getSourceType())
                .score(candidate.getScore())
                .conversationId(candidate.getConversationId())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }

    protected abstract MemoryScopeEnum scope();

    protected abstract int scopeOrder();
}
