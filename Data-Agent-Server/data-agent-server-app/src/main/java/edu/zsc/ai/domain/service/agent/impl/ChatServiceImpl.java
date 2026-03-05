package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.RequestContext;
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
import java.util.Set;

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

        ReActAgent agent = reActAgentProvider.getAgent(modelName);

        if (request.getConversationId() == null) {
            Long userId = RequestContext.getUserId();
            AiConversation conversation = aiConversationService.createConversation(userId, request.getMessage());
            request.setConversationId(conversation.getId());
            RequestContext.updateConversationId(conversation.getId());
            log.info("Created new conversation: id={}", conversation.getId());
        }

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        String memoryId = RequestContext.getUserId() + ":" + request.getConversationId();
        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        String enrichedMessage = buildMessageWithMemoryContext(
                RequestContext.getUserId(),
                request.getConversationId(),
                request.getMessage());
        TokenStream tokenStream = agent.chat(memoryId, enrichedMessage, parameters);

        // Stream token callbacks (inlined from streamTokenStreamToSink)
        Long conversationId = request.getConversationId();

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

        // Track tool call IDs that have been streamed to avoid duplicates in onIntermediateResponse
        final Set<String> streamedToolCallIds = new HashSet<>();

        tokenStream.onPartialToolCallWithContext((partialToolCall, context) -> {
            log.debug("Partial tool call: index={}, id={}, name={}, partialArgs='{}'",
                    partialToolCall.index(), partialToolCall.id(), partialToolCall.name(),
                    partialToolCall.partialArguments());

            // Mark this tool call ID as streamed
            if (partialToolCall.id() != null) {
                streamedToolCallIds.add(partialToolCall.id());
            }

            sink.tryEmitNext(ChatResponseBlock.toolCall(
                    partialToolCall.id(),
                    partialToolCall.name(),
                    partialToolCall.partialArguments(),
                    true  // streaming=true
            ));
        });

        tokenStream.onIntermediateResponse(response -> {
            if (response.aiMessage().hasToolExecutionRequests()) {
                for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                    // Skip if this tool call was already streamed via onPartialToolCall
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
                            false  // streaming=false, arguments complete
                    ));
                }
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();

            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed()));
        });

        tokenStream.onCompleteResponse(response -> {
            // Extract and persist token usage
            if (response.tokenUsage() != null) {
                Integer outputTokens = response.tokenUsage().outputTokenCount();
                Integer totalTokens = response.tokenUsage().totalTokenCount();

                if (totalTokens != null && totalTokens > 0) {
                    log.info("Chat completed for conversation {}: {} total tokens (output: {})",
                            conversationId, totalTokens, outputTokens);

                    // Update the last AI message's token count (output tokens only)
                    if (outputTokens != null && outputTokens > 0) {
                        aiMessageService.updateLastAiMessageTokenCount(conversationId, outputTokens);
                    }

                    // Update conversation with total tokens (includes input + output)
                    aiConversationService.updateTokenCount(conversationId, totalTokens);
                } else {
                    log.debug("No token usage available for conversation {}", conversationId);
                }
            }

            sink.tryEmitNext(ChatResponseBlock.doneBlock(conversationId));
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
            ModelEnum.fromModelName(modelName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ChatErrorConstants.UNKNOWN_MODEL_PREFIX + modelName, e);
        }
        return modelName;
    }

    private String buildMessageWithMemoryContext(Long userId, Long conversationId, String userMessage) {
        if (!memoryProperties.isEnabled() || userId == null || conversationId == null) {
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
