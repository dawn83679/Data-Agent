package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.constant.HitlConstants;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.model.request.ChatRequest;
import edu.zsc.ai.model.request.SubmitToolAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_MODEL = ModelEnum.QWEN3_MAX.getModelName();

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final AiMessageService aiMessageService;
    private final ChatMemoryStore chatMemoryStore;
    private final Map<String, String> mcpToolNameToServerMap;

    public ChatServiceImpl(
            ReActAgentProvider reActAgentProvider,
            AiConversationService aiConversationService,
            AiMessageService aiMessageService,
            ChatMemoryStore chatMemoryStore,
            @Qualifier("mcpToolNameToServerMap") Map<String, String> mcpToolNameToServerMap) {
        this.reActAgentProvider = reActAgentProvider;
        this.aiConversationService = aiConversationService;
        this.aiMessageService = aiMessageService;
        this.chatMemoryStore = chatMemoryStore;
        this.mcpToolNameToServerMap = mcpToolNameToServerMap;
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
        TokenStream tokenStream = agent.chat(memoryId, request.getMessage(), parameters);
        streamTokenStreamToSink(tokenStream, sink, request.getConversationId());
        return sink.asFlux();
    }

    @Override
    public Flux<ChatResponseBlock> submitToolAnswerAndContinue(SubmitToolAnswerRequest request) {
        String modelName = validateAndResolveModel(request.getModel());

        if (request.getConversationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ChatErrorConstants.CONVERSATION_ID_REQUIRED);
        }

        ReActAgent agent = reActAgentProvider.getAgent(modelName);

        Long userId = RequestContext.getUserId();
        String memoryId = userId + ":" + request.getConversationId();

        List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);
        if (messages == null || messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ChatErrorConstants.NO_MESSAGES_FOR_CONVERSATION);
        }

        String toolCallId = request.getToolCallId();
        String answer = request.getAnswer();
        boolean replaced = false;
        List<ChatMessage> newMessages = new ArrayList<>(messages);

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof ToolExecutionResultMessage toolMsg) {
                if (HitlConstants.ASK_USER_QUESTION_TOOL_NAME.equals(toolMsg.toolName())
                        && toolCallId.equals(toolMsg.id())) {
                    ToolExecutionResultMessage replacedMsg = ToolExecutionResultMessage.from(
                            toolMsg.id(), toolMsg.toolName(), answer);
                    newMessages.set(i, replacedMsg);
                    replaced = true;
                    break;
                }
            }
        }

        if (!replaced) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ChatErrorConstants.NO_MATCHING_ASK_USER_TOOL_RESULT_PREFIX + toolCallId);
        }

        chatMemoryStore.updateMessages(memoryId, newMessages);

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        TokenStream tokenStream = agent.chat(memoryId, HitlConstants.HITL_CONTINUE_MESSAGE, parameters);
        streamTokenStreamToSink(tokenStream, sink, request.getConversationId());
        return sink.asFlux();
    }

    private void streamTokenStreamToSink(TokenStream tokenStream,
                                         Sinks.Many<ChatResponseBlock> sink,
                                         Long conversationId) {
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
            String serverName = mcpToolNameToServerMap.get(partialToolCall.name());
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
                    serverName,
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

                    // Query mapping table for MCP server name
                    String serverName = mcpToolNameToServerMap.get(toolRequest.name());
                    log.debug("Complete tool call (non-streaming provider): id={}, name={}",
                            toolRequest.id(), toolRequest.name());

                    sink.tryEmitNext(ChatResponseBlock.toolCall(
                            toolRequest.id(),
                            toolRequest.name(),
                            toolRequest.arguments(),
                            serverName,
                            false  // streaming=false, arguments complete
                    ));
                }
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();
            // Query mapping table for MCP server name
            String serverName = mcpToolNameToServerMap.get(req.name());

            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed(),
                    serverName));
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
}
