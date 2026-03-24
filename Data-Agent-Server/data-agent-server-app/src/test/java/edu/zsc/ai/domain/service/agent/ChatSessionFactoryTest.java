package edu.zsc.ai.domain.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.prompt.PromptRenderResult;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptManager;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import edu.zsc.ai.observability.config.AgentObservabilityConfigProvider;
import edu.zsc.ai.observability.config.AgentObservabilitySettings;

class ChatSessionFactoryTest {

    private final ReActAgentProvider reActAgentProvider = mock(ReActAgentProvider.class);
    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final MemoryContextService memoryContextService = mock(MemoryContextService.class);
    private final UserPromptManager userPromptManager = mock(UserPromptManager.class);
    private final AgentLogService agentLogService = mock(AgentLogService.class);
    private final AgentObservabilityConfigProvider observabilityConfigProvider = mock(AgentObservabilityConfigProvider.class);

    private final ChatSessionFactory factory = new ChatSessionFactory(
            reActAgentProvider,
            aiConversationService,
            memoryContextService,
            userPromptManager,
            agentLogService,
            observabilityConfigProvider);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        AgentRequestContext.clear();
    }

    @Test
    void create_logsFullUserPromptPayloadsWhenIncludePromptEnabled() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(1L)
                .connectionId(12L)
                .catalog("analytics")
                .schema("public")
                .build());

        ChatRequest request = new ChatRequest();
        request.setMessage("我喜欢中文交互");
        request.setLanguage("zh");
        request.setModel("qwen3-max");

        when(reActAgentProvider.getAgent(eq("qwen3-max"), eq("zh"), eq(AgentModeEnum.AGENT.getCode())))
                .thenReturn(mock(ReActAgent.class));
        when(aiConversationService.createConversation(1L, "我喜欢中文交互"))
                .thenReturn(AiConversation.builder().id(99L).userId(1L).title("我喜欢中文交互").build());
        when(memoryContextService.loadPromptContext(1L, 99L, "我喜欢中文交互"))
                .thenReturn(MemoryPromptContext.builder().build());
        when(userPromptManager.render(any()))
                .thenReturn(new PromptRenderResult<>(
                        """
                                <system_context purpose="runtime_environment" apply_to="time_interpretation" strength="reference">
                                当前运行时环境：
                                - today: 2026-03-19
                                - timezone: Asia/Shanghai
                                </system_context>
                                <task purpose="current_user_goal" apply_to="planning,answer_target" strength="highest">
                                当前任务：
                                - 我喜欢中文交互
                                </task>
                                <response_preferences purpose="final_response_preferences" apply_to="language,format,visualization" strength="default">
                                请默认遵循以下偏好：
                                - none
                                </response_preferences>
                                <scope_hints purpose="query_scope_guidance" apply_to="tool_selection,object_search,sql_scope" strength="strong">
                                请优先按以下范围理解和检索：
                                - none
                                </scope_hints>
                                <durable_facts purpose="verified_background_facts" apply_to="reasoning,sql_generation" strength="reference">
                                已知事实：
                                - none
                                </durable_facts>
                                <explicit_references purpose="user_explicit_object_selection" apply_to="scope_resolution,object_priority" strength="highest">
                                本轮用户显式引用：
                                - none
                                </explicit_references>
                                """,
                        promptSections(),
                        42,
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, TASK, RESPONSE_PREFERENCES, SCOPE_HINTS, DURABLE_FACTS, EXPLICIT_REFERENCES"));
        when(observabilityConfigProvider.current()).thenReturn(AgentObservabilitySettings.builder()
                .enabled(true)
                .runtimeLogEnabled(true)
                .consoleLogEnabled(true)
                .includePrompt(true)
                .build());

        ChatSession session = factory.create(request);

        assertNotNull(session);
        assertEquals(99L, session.conversationId());
        assertEquals("qwen3-max", session.modelName());

        Map<AgentLogType, AgentLogEvent> events = capturePromptEvents();

        AgentLogEvent originalEvent = events.get(AgentLogType.PROMPT_ORIGINAL_USER_INPUT);
        assertEquals("我喜欢中文交互", originalEvent.getPayload().get("message"));
        assertEquals("zh", originalEvent.getPayload().get("language"));
        assertEquals("qwen3-max", originalEvent.getPayload().get("modelName"));

        AgentLogEvent renderedEvent = events.get(AgentLogType.PROMPT_RENDERED_USER);
        assertEquals(session.enrichedMessage(), renderedEvent.getPayload().get("prompt"));
        assertEquals(42, renderedEvent.getPayload().get("estimatedTokens"));

        AgentLogEvent sectionsEvent = events.get(AgentLogType.PROMPT_RENDERED_USER_SECTIONS);
        @SuppressWarnings("unchecked")
        Map<String, Object> sections = (Map<String, Object>) sectionsEvent.getPayload().get("sections");
        @SuppressWarnings("unchecked")
        Map<String, Object> taskSection = (Map<String, Object>) sections.get(UserPromptSection.TASK.name());
        assertEquals(Boolean.TRUE, taskSection.get("rendered"));
        assertEquals("当前任务：\n- 我喜欢中文交互", taskSection.get("content"));
        assertEquals(List.of("SYSTEM_CONTEXT", "TASK", "RESPONSE_PREFERENCES", "SCOPE_HINTS", "DURABLE_FACTS", "EXPLICIT_REFERENCES"),
                sectionsEvent.getPayload().get("renderedSections"));
    }

    @Test
    void create_logsOnlyPromptMetadataWhenIncludePromptDisabled() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(1L)
                .build());

        String originalMessage = "show me sales";
        String renderedPrompt = """
                <system_context purpose="runtime_environment" apply_to="time_interpretation" strength="reference">
                Current runtime environment:
                - today: 2026-03-19
                - timezone: Asia/Shanghai
                </system_context>
                <task purpose="current_user_goal" apply_to="planning,answer_target" strength="highest">
                Current task:
                - show me sales
                </task>
                """;
        ChatRequest request = new ChatRequest();
        request.setMessage(originalMessage);
        request.setLanguage("en");

        when(reActAgentProvider.getAgent(eq("qwen3.5-plus"), eq("en"), eq(AgentModeEnum.AGENT.getCode())))
                .thenReturn(mock(ReActAgent.class));
        when(aiConversationService.createConversation(1L, originalMessage))
                .thenReturn(AiConversation.builder().id(77L).userId(1L).title(originalMessage).build());
        when(memoryContextService.loadPromptContext(1L, 77L, originalMessage))
                .thenReturn(MemoryPromptContext.builder().build());
        when(userPromptManager.render(any()))
                .thenReturn(new PromptRenderResult<>(
                        renderedPrompt,
                        promptSections(),
                        11,
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, TASK, RESPONSE_PREFERENCES, SCOPE_HINTS, DURABLE_FACTS, EXPLICIT_REFERENCES"));
        when(observabilityConfigProvider.current()).thenReturn(AgentObservabilitySettings.builder()
                .enabled(true)
                .runtimeLogEnabled(true)
                .includePrompt(false)
                .build());

        factory.create(request);

        Map<AgentLogType, AgentLogEvent> events = capturePromptEvents();

        AgentLogEvent originalEvent = events.get(AgentLogType.PROMPT_ORIGINAL_USER_INPUT);
        assertEquals(originalMessage.length(), originalEvent.getPayload().get("originalLength"));
        assertFalse(originalEvent.getPayload().containsKey("message"));

        AgentLogEvent renderedEvent = events.get(AgentLogType.PROMPT_RENDERED_USER);
        assertEquals(renderedPrompt.length(), renderedEvent.getPayload().get("renderedLength"));
        assertFalse(renderedEvent.getPayload().containsKey("prompt"));

        AgentLogEvent sectionsEvent = events.get(AgentLogType.PROMPT_RENDERED_USER_SECTIONS);
        @SuppressWarnings("unchecked")
        Map<String, Object> sections = (Map<String, Object>) sectionsEvent.getPayload().get("sections");
        @SuppressWarnings("unchecked")
        Map<String, Object> systemContextSection = (Map<String, Object>) sections.get(UserPromptSection.SYSTEM_CONTEXT.name());
        assertEquals(Boolean.TRUE, systemContextSection.get("rendered"));
        assertEquals("当前运行时环境：\n- today: 2026-03-19\n- timezone: Asia/Shanghai", systemContextSection.get("content"));
    }

    @Test
    void create_skipsPromptLoggingWhenRuntimeLogIsDisabled() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(1L)
                .build());

        ChatRequest request = new ChatRequest();
        request.setMessage("show me sales");
        request.setLanguage("en");

        when(reActAgentProvider.getAgent(eq("qwen3.5-plus"), eq("en"), eq(AgentModeEnum.AGENT.getCode())))
                .thenReturn(mock(ReActAgent.class));
        when(aiConversationService.createConversation(1L, "show me sales"))
                .thenReturn(AiConversation.builder().id(66L).userId(1L).title("show me sales").build());
        when(memoryContextService.loadPromptContext(anyLong(), anyLong(), anyString()))
                .thenReturn(MemoryPromptContext.builder().build());
        when(userPromptManager.render(any()))
                .thenReturn(new PromptRenderResult<>(
                        """
                                <task purpose="current_user_goal" apply_to="planning,answer_target" strength="highest">
                                Current task:
                                - show me sales
                                </task>
                                """,
                        promptSections(),
                        11,
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, TASK, RESPONSE_PREFERENCES, SCOPE_HINTS, DURABLE_FACTS, EXPLICIT_REFERENCES"));
        when(observabilityConfigProvider.current()).thenReturn(AgentObservabilitySettings.builder()
                .enabled(true)
                .runtimeLogEnabled(false)
                .includePrompt(true)
                .build());

        factory.create(request);

        verify(agentLogService, never()).record(any(AgentLogEvent.class));
    }

    private Map<AgentLogType, AgentLogEvent> capturePromptEvents() {
        ArgumentCaptor<AgentLogEvent> captor = ArgumentCaptor.forClass(AgentLogEvent.class);
        verify(agentLogService, times(3)).record(captor.capture());
        return captor.getAllValues().stream()
                .collect(Collectors.toMap(AgentLogEvent::getType, Function.identity()));
    }

    private Map<UserPromptSection, PromptSectionResult<UserPromptSection>> promptSections() {
        Map<UserPromptSection, PromptSectionResult<UserPromptSection>> sections = new EnumMap<>(UserPromptSection.class);
        sections.put(UserPromptSection.SYSTEM_CONTEXT, new PromptSectionResult<>(
                UserPromptSection.SYSTEM_CONTEXT,
                "当前运行时环境：\n- today: 2026-03-19\n- timezone: Asia/Shanghai",
                true,
                Map.of("source", "system")));
        sections.put(UserPromptSection.TASK, new PromptSectionResult<>(
                UserPromptSection.TASK,
                "当前任务：\n- 我喜欢中文交互",
                true,
                Map.of()));
        sections.put(UserPromptSection.RESPONSE_PREFERENCES, new PromptSectionResult<>(
                UserPromptSection.RESPONSE_PREFERENCES,
                "请默认遵循以下偏好：\n- none",
                true,
                Map.of()));
        sections.put(UserPromptSection.SCOPE_HINTS, new PromptSectionResult<>(
                UserPromptSection.SCOPE_HINTS,
                "请优先按以下范围理解和检索：\n- none",
                true,
                Map.of()));
        sections.put(UserPromptSection.DURABLE_FACTS, new PromptSectionResult<>(
                UserPromptSection.DURABLE_FACTS,
                "已知事实：\n- none",
                true,
                Map.of()));
        sections.put(UserPromptSection.EXPLICIT_REFERENCES, new PromptSectionResult<>(
                UserPromptSection.EXPLICIT_REFERENCES,
                "本轮用户显式引用：\n- none",
                true,
                Map.of()));
        return sections;
    }
}
