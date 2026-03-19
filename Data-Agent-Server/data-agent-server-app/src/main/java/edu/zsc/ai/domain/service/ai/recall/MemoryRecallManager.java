package edu.zsc.ai.domain.service.ai.recall;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;

@Component
public class MemoryRecallManager {

    private final MemoryService memoryService;
    private final List<MemoryRecallHandler> handlers;
    private final MemoryRecallPostProcessor postProcessor;

    public MemoryRecallManager(MemoryService memoryService,
                               List<MemoryRecallHandler> handlers,
                               MemoryRecallPostProcessor postProcessor) {
        this.memoryService = memoryService;
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(MemoryRecallHandler::order))
                .toList();
        this.postProcessor = postProcessor;
    }

    public MemoryRecallResult recall(MemoryRecallContext input) {
        if (input == null) {
            return MemoryRecallResult.empty();
        }

        MemoryRecallContext context = normalize(input);
        List<MemorySearchResult> candidates = memoryService.recallAccessibleMemories(
                context.getConversationId(),
                context.getQueryText(),
                context.getMinScore() == null ? 0.0D : context.getMinScore());

        List<MemoryRecallItem> merged = handlers.stream()
                .filter(handler -> handler.support(context))
                .flatMap(handler -> handler.handle(context, candidates).stream())
                .toList();

        return postProcessor.process(context, merged);
    }

    private MemoryRecallContext normalize(MemoryRecallContext input) {
        return input.toBuilder()
                .scope(normalizeToken(input.getScope()))
                .memoryType(normalizeToken(input.getMemoryType()))
                .subType(normalizeToken(input.getSubType()))
                .build();
    }

    private String normalizeToken(String value) {
        return StringUtils.isBlank(value) ? null : value.trim().toUpperCase();
    }
}
