package edu.zsc.ai.agent.tool.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.aspect.AgentToolContextAspect;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.MemoryOperationEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;

class UpdateMemoryToolTest {

    private final MemoryService memoryService = mock(MemoryService.class);
    private final UpdateMemoryTool tool = new UpdateMemoryTool(memoryService);

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
    }

    @Test
    void mainAgentCanCreateMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());
        when(memoryService.mutateAgentMemory(any())).thenReturn(MemoryWriteResult.builder()
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

        AgentToolResult result = tool.updateMemory(
                MemoryOperationEnum.CREATE,
                null,
                MemoryScopeEnum.USER,
                MemoryTypeEnum.PREFERENCE,
                MemorySubTypeEnum.RESPONSE_FORMAT,
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
        when(memoryService.mutateAgentMemory(any())).thenReturn(MemoryWriteResult.builder()
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

        AgentToolResult result = tool.updateMemory(
                MemoryOperationEnum.UPDATE,
                8L,
                MemoryScopeEnum.USER,
                MemoryTypeEnum.PREFERENCE,
                MemorySubTypeEnum.RESPONSE_FORMAT,
                "Structured output",
                "User prefers structured output.",
                null,
                InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertEquals("UPDATED", ((java.util.Map<?, ?>) result.getResult()).get("action"));
    }

    @Test
    void plannerCannotUpdateMemory() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        AgentToolResult result = tool.updateMemory(
                MemoryOperationEnum.CREATE,
                null,
                MemoryScopeEnum.USER,
                MemoryTypeEnum.PREFERENCE,
                MemorySubTypeEnum.RESPONSE_FORMAT,
                "Structured output",
                "User prefers structured output.",
                null,
                InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("only available to the main agent"));
    }

    @Test
    void invocationParametersRestoreUserContextForUpdateMemory() {
        UpdateMemoryTool proxy = proxy(new UpdateMemoryTool(memoryService));
        when(memoryService.mutateAgentMemory(any())).thenAnswer(invocation -> {
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

        AgentToolResult result = proxy.updateMemory(
                MemoryOperationEnum.CREATE,
                null,
                MemoryScopeEnum.USER,
                MemoryTypeEnum.PREFERENCE,
                MemorySubTypeEnum.LANGUAGE_PREFERENCE,
                "用户语言偏好",
                "用户偏好使用中文进行交互",
                "用户明确表达了中文交互的偏好",
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.USER_ID, "42",
                        InvocationContextConstant.AGENT_TYPE, "main",
                        InvocationContextConstant.AGENT_MODE, "agent"
                )));

        assertTrue(result.isSuccess());
        verify(memoryService).mutateAgentMemory(any());
    }

    @Test
    void toolGuidance_requiresExecutableScopeIdentifiersForObjectKnowledge() throws Exception {
        Method method = UpdateMemoryTool.class.getMethod(
                "updateMemory",
                MemoryOperationEnum.class,
                Long.class,
                MemoryScopeEnum.class,
                MemoryTypeEnum.class,
                MemorySubTypeEnum.class,
                String.class,
                String.class,
                String.class,
                InvocationParameters.class
        );

        Tool annotation = method.getAnnotation(Tool.class);
        assertNotNull(annotation);

        String joined = String.join("\n", annotation.value());
        assertTrue(joined.contains("readMemory first"),
                "UpdateMemoryTool guidance should require readMemory before UPDATE or DELETE when memoryId is unknown");
        assertTrue(joined.contains("memoryId"),
                "UpdateMemoryTool guidance should describe memoryId for targeted mutations");
        assertTrue(joined.contains("exact identifiers"),
                "UpdateMemoryTool guidance should require exact identifiers for concrete objects");
        assertFalse(joined.contains("chat2db_user"),
                "UpdateMemoryTool guidance should not hardcode a specific object example");
    }

    private static UpdateMemoryTool proxy(UpdateMemoryTool target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        return factory.getProxy();
    }
}
