package edu.zsc.ai.domain.service.agent;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.agent.memory.MemoryIdUtil;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Prepares all state needed before an agent invocation:
 * model resolution, agent-mode setup, conversation creation,
 * memory-id construction, message enrichment, and context snapshot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionFactory {

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final MemoryContextService memoryContextService;

    /**
     * Build a ChatSession from an incoming ChatRequest.
     */
    public ChatSession create(ChatRequest request) {
        String modelName = resolveModel(request.getModel());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(request.getAgentType());

        RequestContext.get().setAgentMode(agentMode.getCode());
        RequestContext.get().setModelName(modelName);
        RequestContext.get().setLanguage(request.getLanguage());

        ReActAgent agent = reActAgentProvider.getAgent(modelName, request.getLanguage(), agentMode.getCode());

        Long conversationId = ensureConversation(request);

        String memoryId = MemoryIdUtil.build(RequestContext.getUserId(), conversationId, modelName);
        String enrichedMessage = memoryContextService.buildEnrichedMessage(
                RequestContext.getUserId(), conversationId, request.getMessage());
        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        RequestContextInfo contextSnapshot = RequestContext.get();

        return new ChatSession(modelName, agentMode, agent, memoryId,
                enrichedMessage, parameters, conversationId, contextSnapshot);
    }

    /**
     * Build a follow-up ChatSession for Plan-mode continuation,
     * reusing the conversation and model from the original session.
     */
    public ChatSession createPlanContinuation(ChatSession original, ChatRequest request) {
        return createContinuation(
                original,
                request.getLanguage(),
                "Continue analyzing the user's request and create a structured execution plan.",
                AgentModeEnum.PLAN);
    }

    public ChatSession createContinuation(
            ChatSession original,
            String language,
            String message,
            AgentModeEnum targetMode) {
        return createContinuation(original, language, message, targetMode, null);
    }

    public ChatSession createContinuation(
            ChatSession original,
            String language,
            String message,
            AgentModeEnum targetMode,
            RequestContextInfo contextOverride) {
        RequestContextInfo base = contextOverride != null ? contextOverride : original.contextSnapshot();
        RequestContextInfo continuationContext = RequestContextInfo.builder()
                .conversationId(base.getConversationId())
                .userId(base.getUserId())
                .connectionId(base.getConnectionId())
                .catalog(base.getCatalog())
                .schema(base.getSchema())
                .modelName(base.getModelName())
                .language(language != null ? language : base.getLanguage())
                .agentMode(targetMode.getCode())
                .runId(base.getRunId())
                .taskId(base.getTaskId())
                .agentRole(base.getAgentRole())
                .parentAgentRole(base.getParentAgentRole())
                .build();
        RequestContext.set(continuationContext);

        ReActAgent agent = reActAgentProvider.getAgent(
                original.modelName(), language, targetMode.getCode());
        InvocationParameters params = InvocationParameters.from(RequestContext.toMap());

        return new ChatSession(original.modelName(), targetMode, agent,
                original.memoryId(), message, params,
                original.conversationId(), continuationContext);
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
}
