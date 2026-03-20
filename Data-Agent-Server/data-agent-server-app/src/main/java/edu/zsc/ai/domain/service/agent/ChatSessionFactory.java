package edu.zsc.ai.domain.service.agent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.agent.memory.MemoryIdUtil;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.PromptRenderResult;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptManager;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import edu.zsc.ai.observability.config.AgentObservabilityConfigProvider;
import edu.zsc.ai.observability.config.AgentObservabilitySettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Prepares all state needed before an agent invocation:
 * model resolution, agent-mode setup, conversation creation,
 * memory-id construction, message enrichment, and context snapshot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionFactory {

    private static final String LOGGER_NAME = "ChatSessionFactory";
    private static final String PAYLOAD_MESSAGE = "message";
    private static final String PAYLOAD_LANGUAGE = "language";
    private static final String PAYLOAD_MODEL_NAME = "modelName";
    private static final String PAYLOAD_AGENT_MODE = "agentMode";
    private static final String PAYLOAD_PROMPT = "prompt";
    private static final String PAYLOAD_ESTIMATED_TOKENS = "estimatedTokens";
    private static final String PAYLOAD_DEBUG_SUMMARY = "debugSummary";
    private static final String PAYLOAD_ORIGINAL_LENGTH = "originalLength";
    private static final String PAYLOAD_RENDERED_LENGTH = "renderedLength";
    private static final String PAYLOAD_RENDERED_SECTIONS = "renderedSections";
    private static final String PAYLOAD_EMPTY_SECTIONS = "emptySections";
    private static final String PAYLOAD_SECTIONS = "sections";
    private static final String PAYLOAD_RENDERED = "rendered";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_METADATA = "metadata";
    private static final String EVENT_ORIGINAL_USER_INPUT = "prompt_original_user_input";
    private static final String EVENT_RENDERED_USER_PROMPT = "prompt_rendered_user";
    private static final String EVENT_RENDERED_USER_SECTIONS = "prompt_rendered_user_sections";

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final MemoryContextService memoryContextService;
    private final UserPromptManager userPromptManager;
    private final AgentLogService agentLogService;
    private final AgentObservabilityConfigProvider observabilityConfigProvider;

    /**
     * Build a ChatSession from an incoming ChatRequest.
     */
    public ChatSession create(ChatRequest request) {
        RequestContextInfo previousRequestContext = RequestContext.snapshot();
        AgentRequestContextInfo previousAgentRequestContext = AgentRequestContext.snapshot();
        try {
            String modelName = resolveModel(request.getModel());
            AgentModeEnum agentMode = AgentModeEnum.fromRequest(request.getAgentType());
            AgentRequestContext.set(AgentRequestContextInfo.builder()
                    .agentMode(agentMode.getCode())
                    .agentType(AgentTypeEnum.MAIN.getCode())
                    .modelName(modelName)
                    .language(request.getLanguage())
                    .build());

            ReActAgent agent = reActAgentProvider.getAgent(modelName, request.getLanguage(), agentMode.getCode());

            Long conversationId = ensureConversation(request);

            String memoryId = MemoryIdUtil.build(RequestContext.getUserId(), conversationId, modelName);
            MemoryPromptContext memoryPromptContext = memoryContextService.loadPromptContext(
                    RequestContext.getUserId(), conversationId, request.getMessage());
            UserPromptAssemblyContext promptContext = UserPromptAssemblyContext.builder()
                    .userMessage(request.getMessage())
                    .language(request.getLanguage())
                    .agentMode(agentMode.promptMode())
                    .modelName(modelName)
                    .currentDate(LocalDate.now(ZoneId.systemDefault()))
                    .timezone(ZoneId.systemDefault().getId())
                    .memoryPromptContext(memoryPromptContext)
                    .userMentions(request.getUserMentions() == null ? List.of() : request.getUserMentions())
                    .build();
            PromptRenderResult<?> promptRenderResult = userPromptManager.render(promptContext);
            String enrichedMessage = promptRenderResult.renderedPrompt();
            recordUserPromptObservability(request, conversationId, modelName, agentMode, promptRenderResult);
            InvocationParameters parameters = InvocationParameters.from(buildInvocationContext());
            RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
            AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();

            return new ChatSession(modelName, agentMode, agent, memoryId,
                    enrichedMessage, parameters, conversationId, requestContextSnapshot, agentRequestContextSnapshot);
        } finally {
            restoreContexts(previousRequestContext, previousAgentRequestContext);
        }
    }

    /**
     * Build a follow-up ChatSession for Plan-mode continuation,
     * reusing the conversation and model from the original session.
     */
    public ChatSession createPlanContinuation(ChatSession original, ChatRequest request) {
        RequestContextInfo previousRequestContext = RequestContext.snapshot();
        AgentRequestContextInfo previousAgentRequestContext = AgentRequestContext.snapshot();
        try {
            if (original.requestContextSnapshot() != null) {
                RequestContext.set(original.requestContextSnapshot());
            } else {
                RequestContext.clear();
            }

            AgentRequestContextInfo planAgentContext = original.agentRequestContextSnapshot() != null
                    ? original.agentRequestContextSnapshot().toBuilder()
                    .agentMode(AgentModeEnum.PLAN.getCode())
                    .build()
                    : AgentRequestContextInfo.builder()
                    .agentMode(AgentModeEnum.PLAN.getCode())
                    .agentType(AgentTypeEnum.MAIN.getCode())
                    .modelName(original.modelName())
                    .language(request.getLanguage())
                    .build();
            AgentRequestContext.set(planAgentContext);

            ReActAgent planAgent = reActAgentProvider.getAgent(
                    original.modelName(), request.getLanguage(), AgentModeEnum.PLAN.getCode());
            InvocationParameters planParams = InvocationParameters.from(buildInvocationContext());
            String continuation = "Continue analyzing the user's request and create a structured execution plan.";

            return new ChatSession(original.modelName(), AgentModeEnum.PLAN, planAgent,
                    original.memoryId(), continuation, planParams,
                    original.conversationId(), RequestContext.snapshot(), AgentRequestContext.snapshot());
        } finally {
            restoreContexts(previousRequestContext, previousAgentRequestContext);
        }
    }

    private String resolveModel(String model) {
        try {
            return ModelEnum.resolve(model).getModelName();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ChatErrorConstants.UNKNOWN_MODEL_PREFIX + model, e);
        }
    }

    private Long ensureConversation(ChatRequest request) {
        if (Objects.nonNull(request.getConversationId())) {
            return request.getConversationId();
        }
        Long userId = RequestContext.getUserId();
        AiConversation conversation = aiConversationService.createConversation(userId, request.getMessage());
        request.setConversationId(conversation.getId());
        RequestContext.updateConversationId(conversation.getId());
        log.info("Created new conversation: id={}", conversation.getId());
        return conversation.getId();
    }

    private Map<String, Object> buildInvocationContext() {
        Map<String, Object> invocationContext = new HashMap<>(RequestContext.toMap());
        invocationContext.putAll(AgentRequestContext.toMap());
        return invocationContext;
    }

    private void restoreContexts(RequestContextInfo previousRequestContext,
                                 AgentRequestContextInfo previousAgentRequestContext) {
        if (previousRequestContext != null) {
            RequestContext.set(previousRequestContext);
        } else {
            RequestContext.clear();
        }

        if (previousAgentRequestContext != null) {
            AgentRequestContext.set(previousAgentRequestContext);
        } else {
            AgentRequestContext.clear();
        }
    }

    private void recordUserPromptObservability(ChatRequest request,
                                               Long conversationId,
                                               String modelName,
                                               AgentModeEnum agentMode,
                                               PromptRenderResult<?> promptRenderResult) {
        AgentObservabilitySettings settings = observabilityConfigProvider.current();
        if (!settings.isEnabled() || !settings.isRuntimeLogEnabled() || request == null || promptRenderResult == null) {
            return;
        }

        boolean includePrompt = settings.isIncludePrompt();
        String originalMessage = request.getMessage();
        String renderedPrompt = promptRenderResult.renderedPrompt();

        agentLogService.record(buildPromptEvent(
                AgentLogType.PROMPT_ORIGINAL_USER_INPUT,
                EVENT_ORIGINAL_USER_INPUT,
                conversationId,
                basePromptPayload(request, modelName, agentMode, promptRenderResult),
                includePrompt ? contentPayload(PAYLOAD_MESSAGE, originalMessage)
                        : contentPayload(PAYLOAD_ORIGINAL_LENGTH, safeLength(originalMessage))));

        agentLogService.record(buildPromptEvent(
                AgentLogType.PROMPT_RENDERED_USER,
                EVENT_RENDERED_USER_PROMPT,
                conversationId,
                basePromptPayload(request, modelName, agentMode, promptRenderResult),
                includePrompt ? contentPayload(PAYLOAD_PROMPT, renderedPrompt)
                        : contentPayload(PAYLOAD_RENDERED_LENGTH, safeLength(renderedPrompt))));

        agentLogService.record(buildPromptEvent(
                AgentLogType.PROMPT_RENDERED_USER_SECTIONS,
                EVENT_RENDERED_USER_SECTIONS,
                conversationId,
                mergePayload(
                        basePromptPayload(request, modelName, agentMode, promptRenderResult),
                        sectionPromptPayload(promptRenderResult, includePrompt)),
                Map.of()));
    }

    private AgentLogEvent buildPromptEvent(AgentLogType type,
                                           String message,
                                           Long conversationId,
                                           Map<String, Object> basePayload,
                                           Map<String, Object> contentPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(basePayload);
        payload.putAll(contentPayload);
        return AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(type)
                .loggerName(LOGGER_NAME)
                .conversationId(conversationId)
                .message(message)
                .payload(payload)
                .build();
    }

    private Map<String, Object> basePromptPayload(ChatRequest request,
                                                  String modelName,
                                                  AgentModeEnum agentMode,
                                                  PromptRenderResult<?> promptRenderResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_LANGUAGE, request.getLanguage());
        payload.put(PAYLOAD_MODEL_NAME, modelName);
        payload.put(PAYLOAD_AGENT_MODE, agentMode.getCode());
        payload.put(PAYLOAD_ESTIMATED_TOKENS, promptRenderResult.estimatedTokens());
        payload.put(PAYLOAD_DEBUG_SUMMARY, promptRenderResult.debugSummary());
        return payload;
    }

    private Map<String, Object> sectionPromptPayload(PromptRenderResult<?> promptRenderResult, boolean includePrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_ESTIMATED_TOKENS, promptRenderResult.estimatedTokens());
        payload.put(PAYLOAD_DEBUG_SUMMARY, promptRenderResult.debugSummary());

        Map<String, Object> sectionsPayload = new LinkedHashMap<>();
        List<String> renderedSections = new ArrayList<>();
        List<String> emptySections = new ArrayList<>();

        promptRenderResult.sectionPayloads().forEach((section, result) -> {
            String sectionName = String.valueOf(section);
            PromptSectionResult<?> sectionResult = (PromptSectionResult<?>) result;
            Map<String, Object> sectionPayload = new LinkedHashMap<>();
            boolean rendered = sectionResult != null && sectionResult.rendered();
            sectionPayload.put(PAYLOAD_RENDERED, rendered);
            sectionPayload.put(PAYLOAD_METADATA, sectionResult == null ? Map.of() : sectionResult.metadata());
            sectionPayload.put(PAYLOAD_CONTENT, sectionResult == null ? null : sectionResult.content());
            if (rendered) {
                renderedSections.add(sectionName);
            } else {
                emptySections.add(sectionName);
            }
            sectionsPayload.put(sectionName, sectionPayload);
        });

        payload.put(PAYLOAD_RENDERED_SECTIONS, renderedSections);
        payload.put(PAYLOAD_EMPTY_SECTIONS, emptySections);
        payload.put(PAYLOAD_SECTIONS, sectionsPayload);
        return payload;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private Map<String, Object> contentPayload(String key, Object value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, value);
        return payload;
    }

    private Map<String, Object> mergePayload(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(left);
        payload.putAll(right);
        return payload;
    }

}
