package edu.zsc.ai.domain.service.ai.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallManager;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryContextServiceImpl implements MemoryContextService {

    private final MemoryRecallManager memoryRecallManager;
    private final MemoryService memoryService;
    private final MemoryProperties memoryProperties;

    @Override
    public MemoryPromptContext loadPromptContext(Long userId, Long conversationId, String userMessage) {
        if (!memoryProperties.isEnabled() || Objects.isNull(userId) || Objects.isNull(conversationId)) {
            return MemoryPromptContext.builder().build();
        }

        try {
            MemoryPromptContext promptContext = MemoryPromptContext.builder()
                    .recallResult(memoryRecallManager.recall(MemoryRecallContext.builder()
                            .conversationId(conversationId)
                            .queryText(userMessage)
                            .recallMode(MemoryRecallMode.PROMPT)
                            .minScore(memoryProperties.getRetrieval().getMinScore())
                            .build()))
                    .build();
            List<Long> memoryIds = promptContext.getMemories().stream()
                    .map(MemoryRecallItem::getId)
                    .toList();
            memoryService.recordMemoryAccess(memoryIds);
            memoryService.recordMemoryUsage(memoryIds);
            log.info("Memory prompt recall: conversationId={}, recalledCount={}, memoryIds={}, summary={}",
                    conversationId,
                    memoryIds.size(),
                    memoryIds,
                    promptContext.getRecallResult() == null ? "" : promptContext.getRecallResult().getSummary());
            return promptContext;
        } catch (Exception e) {
            log.warn("Failed to fetch memory context for user {}", userId, e);
            return MemoryPromptContext.builder().build();
        }
    }
}
