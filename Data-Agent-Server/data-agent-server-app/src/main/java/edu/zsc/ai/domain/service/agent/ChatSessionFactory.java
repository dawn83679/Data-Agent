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
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.constant.InvocationContextConstant;
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
import edu.zsc.ai.domain.service.agent.runtimecontext.strategy.ConnectionSummary;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.model.MemoryPromptContext;
import edu.zsc.ai.util.ConnectionIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Prepares all state needed before an agent invocation:
 * model resolution, agent-mode setup, conversation creation,
 * memory-id construction, and context snapshot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionFactory {

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final MemoryContextService memoryContextService;
    private final RuntimeContextManager runtimeContextManager;
    private final DbConnectionService dbConnectionService;
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

            PreparedReActAgent prepared = reActAgentProvider.getAgent(modelName, request.getLanguage(), agentMode.getCode());
            ReActAgent agent = prepared.agent();

            Long conversationId = ensureConversation(request);

            String memoryId = MemoryIdUtil.build(RequestContext.getUserId(), conversationId, modelName);

            List<ConnectionSummary> connections = dbConnectionService.getAllConnections().stream()
                    .map(conn -> new ConnectionSummary(conn.getId(), conn.getName(), conn.getDbType()))
                    .toList();

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

            Map<String, Object> invocationContext = buildInvocationContext();
            if (StringUtils.isNotBlank(runtimeContext)) {
                invocationContext.put("runtimeSystemPromptSuffix", runtimeContext);
            }
            String readableConnCsv = ConnectionIdUtil.toCsv(
                    connections.stream().map(ConnectionSummary::id).filter(Objects::nonNull).toList());
            if (readableConnCsv != null) {
                invocationContext.put(InvocationContextConstant.READABLE_CONNECTION_IDS, readableConnCsv);
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

            PreparedReActAgent planPrepared = reActAgentProvider.getAgent(
                    original.modelName(), request.getLanguage(), AgentModeEnum.PLAN.getCode());
            ReActAgent planAgent = planPrepared.agent();
            List<ConnectionSummary> planConnections = dbConnectionService.getAllConnections().stream()
                    .map(conn -> new ConnectionSummary(conn.getId(), conn.getName(), conn.getDbType()))
                    .toList();
            Map<String, Object> planInvocation = buildInvocationContext();
            String planReadableCsv = ConnectionIdUtil.toCsv(
                    planConnections.stream().map(ConnectionSummary::id).filter(Objects::nonNull).toList());
            if (planReadableCsv != null) {
                planInvocation.put(InvocationContextConstant.READABLE_CONNECTION_IDS, planReadableCsv);
            }
            InvocationParameters planParams = InvocationParameters.from(planInvocation);
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
