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
                                <system_context>
                                today: 2026-03-19
                                </system_context>
                                <user_question>
                                我喜欢中文交互
                                </user_question>
                                """,
                        promptSections(),
                        42,
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, USER_MEMORY, USER_QUESTION"));
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
        Map<String, Object> userQuestionSection = (Map<String, Object>) sections.get(UserPromptSection.USER_QUESTION.name());
        assertEquals(Boolean.TRUE, userQuestionSection.get("rendered"));
        assertEquals("<user_question>\n我喜欢中文交互\n</user_question>", userQuestionSection.get("content"));
        assertEquals(List.of("SYSTEM_CONTEXT", "USER_MEMORY", "USER_QUESTION"), sectionsEvent.getPayload().get("renderedSections"));
    }

    @Test
    void create_logsOnlyPromptMetadataWhenIncludePromptDisabled() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(1L)
                .build());

        String originalMessage = "show me sales";
        String renderedPrompt = "<user_question>\nshow me sales\n</user_question>";
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
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, USER_MEMORY, USER_QUESTION"));
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
        assertEquals("<system_context>\ntoday: 2026-03-19\n</system_context>", systemContextSection.get("content"));
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
                        "<user_question>\nshow me sales\n</user_question>",
                        promptSections(),
                        11,
                        "Rendered runtime prompt sections: SYSTEM_CONTEXT, USER_MEMORY, USER_QUESTION"));
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
                "<system_context>\ntoday: 2026-03-19\n</system_context>",
                true,
                Map.of("source", "system")));
        sections.put(UserPromptSection.USER_MEMORY, new PromptSectionResult<>(
                UserPromptSection.USER_MEMORY,
                "<user_memory>\n- prefers chinese\n</user_memory>",
                true,
                Map.of("memoryCount", 1)));
        sections.put(UserPromptSection.USER_MENTION, new PromptSectionResult<>(
                UserPromptSection.USER_MENTION,
                "",
                false,
                Map.of()));
        sections.put(UserPromptSection.USER_QUESTION, new PromptSectionResult<>(
                UserPromptSection.USER_QUESTION,
                "<user_question>\n我喜欢中文交互\n</user_question>",
                true,
                Map.of()));
        return sections;
    }
}
