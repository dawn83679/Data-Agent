package edu.zsc.ai.agent.tool.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;

class WriteMemoryToolTest {

    private final MemoryService memoryService = mock(MemoryService.class);
    private final WriteMemoryTool tool = new WriteMemoryTool(memoryService);

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
    }

    @Test
    void mainAgentCanWriteMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());
        when(memoryService.writeAgentMemory(any())).thenReturn(AiMemory.builder()
                .id(7L)
                .scope("USER")
                .memoryType("PREFERENCE")
                .subType("OUTPUT_FORMAT")
                .reviewState("NEEDS_REVIEW")
                .title("Structured output")
                .content("User prefers structured output.")
                .createdAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                .build());

        AgentToolResult result = tool.writeMemory(
                "USER",
                null,
                null,
                null,
                null,
                "PREFERENCE",
                "OUTPUT_FORMAT",
                "Structured output",
                "User prefers structured output.",
                "The preference was repeated explicitly.",
                0.92,
                List.of("msg-1", "msg-2"));

        assertTrue(result.isSuccess());
        assertEquals("CREATED", ((java.util.Map<?, ?>) result.getResult()).get("action"));
    }

    @Test
    void plannerCannotWriteMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.writeMemory(
                "USER",
                null,
                null,
                null,
                null,
                "PREFERENCE",
                "OUTPUT_FORMAT",
                "Structured output",
                "User prefers structured output.",
                null,
                null,
                List.of());

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("only available to the main agent"));
    }
}
