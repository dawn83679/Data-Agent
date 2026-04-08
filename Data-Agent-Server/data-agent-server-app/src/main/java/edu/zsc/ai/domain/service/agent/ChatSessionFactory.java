package edu.zsc.ai.domain.service.agent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.PreparedReActAgent;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.agent.memory.MemoryIdUtil;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.constant.AgentRuntimeLoggerNames;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextManager;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares all state needed before an agent invocation:
 * model resolution, agent-mode setup, conversation creation,
 * memory-id construction, and context snapshot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionFactory {

    private static final Logger conversationRuntimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.CONVERSATION);
    private static final Logger promptRuntimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.PROMPT);

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final MemoryContextService memoryContextService;
    private final RuntimeContextManager runtimeContextManager;
    private final AiModelCatalog aiModelCatalog;

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

            Long conversationId = ensureConversation(request);

            String memoryId = MemoryIdUtil.build(RequestContext.getUserId(), conversationId, modelName);

            PreparedReActAgent preparedAgent = reActAgentProvider.getAgent(
                    modelName,
                    request.getLanguage(),
                    agentMode.getCode()
            );
            ReActAgent agent = preparedAgent.agent();

            MemoryPromptContext memoryPromptContext = memoryContextService.loadPromptContext(
                    RequestContext.getUserId(), conversationId, request.getMessage());
            RuntimeContextAssemblyContext runtimeCtx = RuntimeContextAssemblyContext.builder()
                    .language(request.getLanguage())
                    .currentDate(LocalDate.now(ZoneId.systemDefault()))
                    .timezone(ZoneId.systemDefault().getId())
                    .memoryPromptContext(memoryPromptContext)
                    .userMentions(request.getUserMentions() == null ? List.of() : request.getUserMentions())
                    .build();
            String runtimeContext = runtimeContextManager.render(runtimeCtx).renderedPrompt();
            // The runtime suffix carries time, memory projections, and explicit references.
            // Connection inventory is now discovered via getAvailableConnections on demand.
            String systemPrompt = StringUtils.defaultString(preparedAgent.systemPrompt());
            String safeRuntimeContext = StringUtils.defaultString(runtimeContext);
            String combinedPrompt = composeCombinedPrompt(systemPrompt, safeRuntimeContext);

            conversationRuntimeLog.info(
                    "conversation_start conversationId={} userId={} agentMode={} modelName={} language={} messageLength={} message=\n{}",
                    conversationId,
                    RequestContext.getUserId(),
                    agentMode.getCode(),
                    modelName,
                    request.getLanguage(),
                    StringUtils.length(request.getMessage()),
                    StringUtils.defaultString(request.getMessage()));
            promptRuntimeLog.info(
                    "main_prompt_system conversationId={} userId={} agentMode={} modelName={} promptCode={} contentLength={} content=\n{}",
                    conversationId,
                    RequestContext.getUserId(),
                    agentMode.getCode(),
                    modelName,
                    preparedAgent.promptEnum().getCode(),
                    systemPrompt.length(),
                    systemPrompt);
            promptRuntimeLog.info(
                    "main_prompt_runtime_context conversationId={} userId={} agentMode={} modelName={} promptCode={} contentLength={} content=\n{}",
                    conversationId,
                    RequestContext.getUserId(),
                    agentMode.getCode(),
                    modelName,
                    preparedAgent.promptEnum().getCode(),
                    safeRuntimeContext.length(),
                    safeRuntimeContext);
            promptRuntimeLog.info(
                    "main_prompt_combined conversationId={} userId={} agentMode={} modelName={} promptCode={} contentLength={} content=\n{}",
                    conversationId,
                    RequestContext.getUserId(),
                    agentMode.getCode(),
                    modelName,
                    preparedAgent.promptEnum().getCode(),
                    combinedPrompt.length(),
                    combinedPrompt);

            Map<String, Object> invocationContext = buildInvocationContext();
            if (StringUtils.isNotBlank(runtimeContext)) {
                invocationContext.put("runtimeSystemPromptSuffix", runtimeContext);
            }
            InvocationParameters parameters = InvocationParameters.from(invocationContext);
            RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
            AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();

            return new ChatSession(modelName, agentMode, agent, memoryId,
                    request.getMessage(), parameters, conversationId, requestContextSnapshot, agentRequestContextSnapshot);
        } finally {
            restoreContexts(previousRequestContext, previousAgentRequestContext);
        }
    }

    private String resolveModel(String model) {
        try {
            return aiModelCatalog.resolve(model).getModelName();
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

    private String composeCombinedPrompt(String systemPrompt, String runtimeContext) {
        if (StringUtils.isBlank(runtimeContext)) {
            return systemPrompt;
        }
        return systemPrompt + "\n" + runtimeContext;
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
}
