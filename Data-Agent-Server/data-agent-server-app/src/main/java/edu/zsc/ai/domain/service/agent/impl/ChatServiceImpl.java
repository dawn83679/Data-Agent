package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.config.ai.MemoryProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@EnableConfigurationProperties(MemoryProperties.class)
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_MODEL = ModelEnum.QWEN3_MAX.getModelName();

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final AiMessageService aiMessageService;
    private final MemoryService memoryService;
    private final MemoryCandidateService memoryCandidateService;
    private final MemoryProperties memoryProperties;

    public ChatServiceImpl(
            ReActAgentProvider reActAgentProvider,
            AiConversationService aiConversationService,
            AiMessageService aiMessageService,
            MemoryService memoryService,
            MemoryCandidateService memoryCandidateService,
            MemoryProperties memoryProperties) {
        this.reActAgentProvider = reActAgentProvider;
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.memoryService = memoryService;
        this.memoryCandidateService = memoryCandidateService;
        this.memoryProperties = memoryProperties;
    }

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        String modelName = validateAndResolveModel(request.getModel());

        AgentModeEnum agentMode = AgentModeEnum.fromRequest(request.getAgentType());
        // Set agentMode in RequestContext so it flows into InvocationParameters
        if (RequestContext.hasContext()) {
            RequestContext.get().setAgentMode(agentMode.getCode());
        }

        ReActAgent agent = reActAgentProvider.getAgent(modelName, request.getLanguage(), agentMode.getCode());

        if (Objects.isNull(request.getConversationId())) {
            Long userId = RequestContext.getUserId();
            AiConversation conversation = aiConversationService.createConversation(userId, request.getMessage());
            request.setConversationId(conversation.getId());
            RequestContext.updateConversationId(conversation.getId());
            log.info("Created new conversation: id={}", conversation.getId());
        }

        String memoryId = RequestContext.getUserId() + ":" + request.getConversationId();
        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        String enrichedMessage = buildMessageWithMemoryContext(
                RequestContext.getUserId(),
                request.getConversationId(),
                request.getMessage());
        Long conversationId = request.getConversationId();

        // Capture RequestContext snapshot for use in deferred Flux (may run on different thread)
        RequestContextInfo contextSnapshot = RequestContext.hasContext()
                ? RequestContext.get()
                : null;

        AtomicBoolean enterPlanTriggered = new AtomicBoolean(false);

        // First stream: current agent mode (emitDoneBlock=false, defer block handles it)
        Flux<ChatResponseBlock> agentFlux = streamAgent(
                agent, memoryId, enrichedMessage, parameters,
                conversationId, enterPlanTriggered, false);

        // Chain second stream: only when enterPlanMode was triggered
        return agentFlux.concatWith(Flux.defer(() -> {
            if (!enterPlanTriggered.get()) {
                // enterPlanMode not triggered — emit the doneBlock normally
                return Flux.just(ChatResponseBlock.doneBlock());
            }

            log.info("enterPlanMode triggered for conversation {}, chaining Plan mode agent", conversationId);

            // Restore RequestContext on the deferred thread
            if (Objects.nonNull(contextSnapshot)) {
                RequestContext.set(contextSnapshot);
                RequestContext.get().setAgentMode(AgentModeEnum.PLAN.getCode());
            }

            ReActAgent planAgent = reActAgentProvider.getAgent(
                    modelName, request.getLanguage(), AgentModeEnum.PLAN.getCode());
            InvocationParameters planParams = InvocationParameters.from(RequestContext.toMap());
            String continuation = "Continue analyzing the user's request and create a structured execution plan.";

            return streamAgent(planAgent, memoryId, continuation, planParams,
                    conversationId, new AtomicBoolean(false), true);
        })).map(block -> {
            if (Objects.nonNull(block) && Objects.isNull(block.getConversationId())) {
                block.setConversationId(conversationId);
            }
            return block;
        });
    }

    /**
     * Streams a single agent invocation into a Flux of ChatResponseBlocks.
     *
     * @param agent               the agent to invoke
     * @param memoryId            chat memory identifier
     * @param message             the user message (or continuation prompt)
     * @param parameters          invocation parameters (RequestContext snapshot)
     * @param conversationId      conversation ID for token tracking
     * @param enterPlanTriggered  set to true if enterPlanMode tool is executed
     * @param emitDoneBlock       whether to emit doneBlock on completion
     */
    private Flux<ChatResponseBlock> streamAgent(
            ReActAgent agent, String memoryId, String message,
            InvocationParameters parameters, Long conversationId,
            AtomicBoolean enterPlanTriggered, boolean emitDoneBlock) {

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        TokenStream tokenStream = agent.chat(memoryId, message, parameters);
        Set<String> streamedToolCallIds = new HashSet<>();

        tokenStream.onPartialResponse(content -> {
            if (StringUtils.isNotBlank(content)) {
                sink.tryEmitNext(ChatResponseBlock.text(content));
            }
        });

        tokenStream.onPartialThinking(partial -> {
            if (StringUtils.isNotBlank(partial.text())) {
                sink.tryEmitNext(ChatResponseBlock.thought(partial.text()));
            }
        });

        tokenStream.onPartialToolCallWithContext((partialToolCall, context) -> {
            log.debug("Partial tool call: index={}, id={}, name={}, partialArgs='{}'",
                    partialToolCall.index(), partialToolCall.id(), partialToolCall.name(),
                    partialToolCall.partialArguments());

            if (Objects.nonNull(partialToolCall.id())) {
                streamedToolCallIds.add(partialToolCall.id());
            }

            sink.tryEmitNext(ChatResponseBlock.toolCall(
                    partialToolCall.id(),
                    partialToolCall.name(),
                    partialToolCall.partialArguments(),
                    true
            ));
        });

        tokenStream.onIntermediateResponse(response -> {
            if (response.aiMessage().hasToolExecutionRequests()) {
                for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                    if (streamedToolCallIds.contains(toolRequest.id())) {
                        log.debug("Skipping already-streamed tool call: id={}, name={}",
                                toolRequest.id(), toolRequest.name());
                        continue;
                    }

                    log.debug("Complete tool call (non-streaming provider): id={}, name={}",
                            toolRequest.id(), toolRequest.name());

                    sink.tryEmitNext(ChatResponseBlock.toolCall(
                            toolRequest.id(),
                            toolRequest.name(),
                            toolRequest.arguments(),
                            false
                    ));
                }
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();

            // Detect enterPlanMode tool execution
            if (ToolNameEnum.ENTER_PLAN_MODE.getToolName().equals(req.name())) {
                enterPlanTriggered.set(true);
            }

            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed()));
        });

        tokenStream.onCompleteResponse(response -> {
            // Extract and persist token usage
            if (Objects.nonNull(response.tokenUsage())) {
                Integer outputTokens = response.tokenUsage().outputTokenCount();
                Integer totalTokens = response.tokenUsage().totalTokenCount();

                if (Objects.nonNull(totalTokens) && totalTokens > 0) {
                    log.info("Chat completed for conversation {}: {} total tokens (output: {})",
                            conversationId, totalTokens, outputTokens);

                    if (Objects.nonNull(outputTokens) && outputTokens > 0) {
                        aiMessageService.updateLastAiMessageTokenCount(conversationId, outputTokens);
                    }

                    aiConversationService.updateTokenCount(conversationId, totalTokens);
                } else {
                    log.debug("No token usage available for conversation {}", conversationId);
                }
            }

            if (emitDoneBlock) {
                sink.tryEmitNext(ChatResponseBlock.doneBlock());
            }
            sink.tryEmitComplete();
        });

        tokenStream.onError(error -> {
            log.error("Error in chat stream", error);
            sink.tryEmitError(error);
        });

        tokenStream.start();
        return sink.asFlux();
    }

    /**
     * Resolves request model to a valid model name, or DEFAULT_MODEL if blank.
     * Throws ResponseStatusException if the model is not supported.
     */
    private String validateAndResolveModel(String requestModel) {
        String modelName = StringUtils.isNotBlank(requestModel) ? requestModel.trim() : DEFAULT_MODEL;
        try {
            return ModelEnum.fromModelName(modelName).getModelName();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ChatErrorConstants.UNKNOWN_MODEL_PREFIX + modelName, e);
        }
    }

    private String buildMessageWithMemoryContext(Long userId, Long conversationId, String userMessage) {
        if (!memoryProperties.isEnabled() || Objects.isNull(userId) || Objects.isNull(conversationId)) {
            return userMessage;
        }

        MemoryProperties.Retrieval retrieval = memoryProperties.getRetrieval();

        List<MemorySearchResult> memories = List.of();
        try {
            memories = memoryService.searchActiveMemories(
                    userId, userMessage, retrieval.getPreloadTopK(), retrieval.getMinScore());
        } catch (Exception e) {
            log.warn("Failed to fetch memory context for user {}", userId, e);
        }

        List<AiMemoryCandidate> candidates = List.of();
        try {
            candidates = memoryCandidateService.listCurrentConversationCandidates(
                    userId, conversationId, retrieval.getCandidateTopK());
        } catch (Exception e) {
            log.warn("Failed to fetch candidate context for conversation {}", conversationId, e);
        }

        return MemoryUtil.buildEnrichedMessage(userMessage, memories, candidates);
    }
}
