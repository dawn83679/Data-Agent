package edu.zsc.ai.agent.tool.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.MemoryRecallConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallManager;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;

class ReadMemoryToolTest {

    private final MemoryRecallManager memoryRecallManager = mock(MemoryRecallManager.class);
    private final MemoryService memoryService = mock(MemoryService.class);
    private final ReadMemoryTool tool = new ReadMemoryTool(memoryRecallManager, memoryService);

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }

    @Test
    void mainAgentCanReadMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());
        when(memoryRecallManager.recall(any())).thenReturn(MemoryRecallResult.builder()
                .summary("Recalled 1 durable memory item(s) across USER.")
                .items(List.of(MemoryRecallItem.builder()
                        .id(5L)
                        .scope(MemoryScopeEnum.USER.getCode())
                        .memoryType(MemoryTypeEnum.PREFERENCE.getCode())
                        .subType(MemorySubTypeEnum.OUTPUT_FORMAT.getCode())
                        .content("User prefers concise output.")
                        .build()))
                .build());

        AgentToolResult result = tool.readMemory(
                "remember the user's output preference",
                MemoryScopeEnum.USER.getCode(),
                MemoryTypeEnum.PREFERENCE.getCode(),
                MemorySubTypeEnum.OUTPUT_FORMAT.getCode());

        assertTrue(result.isSuccess());
        Map<?, ?> payload = (Map<?, ?>) result.getResult();
        assertEquals("Recalled 1 durable memory item(s) across USER.", payload.get(MemoryRecallConstant.RESULT_SUMMARY));
        assertEquals(1, ((List<?>) payload.get(MemoryRecallConstant.RESULT_ITEMS)).size());
        verify(memoryService).recordMemoryAccess(List.of(5L));
    }

    @Test
    void readMemoryRejectsInvalidSubTypeCombination() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.readMemory(
                "find domain rules",
                null,
                MemoryTypeEnum.PREFERENCE.getCode(),
                MemorySubTypeEnum.DOMAIN_RULE.getCode());

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("does not belong to memoryType"));
    }

    @Test
    void plannerCannotReadMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.readMemory("find workflow constraints", null, null, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("only available to the main agent"));
    }

    @Test
    void readMemoryRequiresIntent() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.readMemory(" ", null, null, null);

        assertFalse(result.isSuccess());
        assertEquals("intent is required for readMemory.", result.getMessage());
    }
}
