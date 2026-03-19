package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

public interface MemoryRecallHandler {

    boolean support(MemoryRecallContext context);

    List<MemoryRecallItem> handle(MemoryRecallContext context, List<MemorySearchResult> candidates);

    int order();
}
