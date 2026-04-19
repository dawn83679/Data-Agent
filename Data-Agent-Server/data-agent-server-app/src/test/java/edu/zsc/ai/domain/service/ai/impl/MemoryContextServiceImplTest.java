package edu.zsc.ai.domain.service.ai.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallManager;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

@ExtendWith(MockitoExtension.class)
class MemoryContextServiceImplTest {

    @Mock
    private MemoryRecallManager memoryRecallManager;

    @Mock
    private MemoryProperties memoryProperties;

    @Mock
    private MemoryProperties.Retrieval retrieval;

    @Mock
    private MemoryService memoryService;

    @InjectMocks
    private MemoryContextServiceImpl memoryContextService;

    @Test
    void loadPromptContext_returnsEmptyWhenMemoryDisabled() {
        when(memoryProperties.isEnabled()).thenReturn(false);

        MemoryPromptContext context = memoryContextService.loadPromptContext(42L, 7L, "show me the latest orders");

        assertTrue(context.getMemories().isEmpty());
        verify(memoryRecallManager, never()).recall(org.mockito.ArgumentMatchers.any(MemoryRecallContext.class));
    }

    @Test
    void loadPromptContext_stillLoadsCurrentConversationMemoryWhenRecallDisabled() {
        when(memoryProperties.isEnabled()).thenReturn(false);
        when(memoryService.getConversationWorkingMemory(42L, 7L)).thenReturn(AiMemory.builder()
                .id(8L)
                .content("# Current Task\nImplement memory writer")
                .build());

        MemoryPromptContext context = memoryContextService.loadPromptContext(42L, 7L, "continue");

        assertEquals("# Current Task\nImplement memory writer", context.getCurrentConversationMemory());
        verify(memoryRecallManager, never()).recall(org.mockito.ArgumentMatchers.any(MemoryRecallContext.class));
    }

    @Test
    void loadPromptContext_returnsRetrievedMemoriesOnly() {
        when(memoryProperties.isEnabled()).thenReturn(true);
        when(memoryProperties.getRetrieval()).thenReturn(retrieval);
        when(retrieval.getMinScore()).thenReturn(0.72);
        when(memoryService.getConversationWorkingMemory(42L, 7L)).thenReturn(AiMemory.builder()
                .id(9L)
                .content("# Current Task\nDesign memory refactor")
                .build());
        when(memoryRecallManager.recall(org.mockito.ArgumentMatchers.any(MemoryRecallContext.class)))
                .thenReturn(MemoryRecallResult.builder()
                        .items(List.of(
                                MemoryRecallItem.builder()
                                        .id(1L)
                                        .scope(MemoryScopeEnum.USER.getCode())
                                        .memoryType("PREFERENCE")
                                        .content("User prefers concise output.")
                                        .score(0.91)
                                        .build()))
                        .build());

        MemoryPromptContext context = memoryContextService.loadPromptContext(42L, 7L, "design a migration plan");

        assertEquals(1, context.getMemories().size());
        assertEquals("User prefers concise output.", context.getMemories().get(0).getContent());
        assertEquals("# Current Task\nDesign memory refactor", context.getCurrentConversationMemory());
        verify(memoryService).recordMemoryAccess(List.of(1L));
    }
}
