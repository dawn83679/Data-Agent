package edu.zsc.ai.agent.tool.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.aspect.AgentToolContextAspect;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;

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
        when(memoryService.writeAgentMemory(any())).thenReturn(MemoryWriteResult.builder()
                .memory(AiMemory.builder()
                        .id(7L)
                        .scope("USER")
                        .memoryType("PREFERENCE")
                        .subType("RESPONSE_FORMAT")
                        .title("Structured output")
                        .content("User prefers structured output.")
                        .createdAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                        .build())
                .action(MemoryToolActionEnum.CREATED)
                .build());

        AgentToolResult result = tool.writeMemory(
                "USER",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                "Structured output",
                "User prefers structured output.",
                "The preference was repeated explicitly.",
                InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertEquals("CREATED", ((java.util.Map<?, ?>) result.getResult()).get("action"));
    }

    @Test
    void toolUsesServiceActionInsteadOfGuessingFromTimestamps() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());
        when(memoryService.writeAgentMemory(any())).thenReturn(MemoryWriteResult.builder()
                .memory(AiMemory.builder()
                        .id(8L)
                        .scope("USER")
                        .memoryType("PREFERENCE")
                        .subType("RESPONSE_FORMAT")
                        .createdAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 19, 10, 0))
                        .build())
                .action(MemoryToolActionEnum.UPDATED)
                .build());

        AgentToolResult result = tool.writeMemory(
                "USER",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                "Structured output",
                "User prefers structured output.",
                null,
                InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertEquals("UPDATED", ((java.util.Map<?, ?>) result.getResult()).get("action"));
    }

    @Test
    void plannerCannotWriteMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.writeMemory(
                "USER",
                "PREFERENCE",
                "RESPONSE_FORMAT",
                "Structured output",
                "User prefers structured output.",
                null,
                InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("only available to the main agent"));
    }

    @Test
    void invocationParametersRestoreUserContextForWriteMemory() {
        WriteMemoryTool proxy = proxy(new WriteMemoryTool(memoryService));
        when(memoryService.writeAgentMemory(any())).thenAnswer(invocation -> {
            assertEquals(42L, RequestContext.getUserId());
            return MemoryWriteResult.builder()
                    .memory(AiMemory.builder()
                            .id(9L)
                            .scope("USER")
                            .memoryType("PREFERENCE")
                            .subType("LANGUAGE_PREFERENCE")
                            .build())
                    .action(MemoryToolActionEnum.CREATED)
                    .build();
        });

        AgentToolResult result = proxy.writeMemory(
                "USER",
                "PREFERENCE",
                "LANGUAGE_PREFERENCE",
                "用户语言偏好",
                "用户偏好使用中文进行交互",
                "用户明确表达了中文交互的偏好",
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.USER_ID, "42",
                        InvocationContextConstant.AGENT_TYPE, "main",
                        InvocationContextConstant.AGENT_MODE, "agent"
                )));

        assertTrue(result.isSuccess());
        verify(memoryService).writeAgentMemory(any());
    }

    private static WriteMemoryTool proxy(WriteMemoryTool target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        return factory.getProxy();
    }
}
