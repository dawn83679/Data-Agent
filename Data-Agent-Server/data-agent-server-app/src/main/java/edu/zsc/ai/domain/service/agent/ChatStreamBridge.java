package edu.zsc.ai.domain.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.tool.AgentToolTracker;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.domain.event.ChatCompletedEvent;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridges a LangChain4j {@link TokenStream} into a reactive
 * {@code Flux<ChatResponseBlock>} suitable for SSE streaming.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamBridge {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentLogService agentLogService;

    /**
     * Start the agent chat from the given session and bridge the resulting
     * {@link TokenStream} into a {@link Flux} of {@link ChatResponseBlock}.
     *
     * @param session            the prepared chat session (agent + parameters)
     * @param enterPlanTriggered set to {@code true} when the enterPlanMode tool fires
     * @param emitDoneBlock      whether to emit a done-block when the stream completes
     * @param toolTracker        shared tracker for recording tool invocations
     */
    public Flux<ChatResponseBlock> bridge(
            ChatSession session,
            AtomicBoolean enterPlanTriggered,
            boolean emitDoneBlock,
            AgentToolTracker toolTracker) {

        Long conversationId = session.conversationId();
        TokenStream tokenStream = session.startChat();

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        sseEmitterRegistry.register(conversationId, sink);
        log.debug("[ChatStream] sink registered for conversation {}", conversationId);
        Set<String> streamedToolCallIds = new HashSet<>();

        tokenStream.onPartialResponse(content -> {
            if (StringUtils.isNotBlank(content)) {
                agentLogService.record(
                        AgentLogType.TOKEN_PARTIAL_RESPONSE,
                        "ChatStreamBridge",
                        "partial_response",
                        AgentLogFields.of("length", content.length(), "preview", content));
                sink.tryEmitNext(ChatResponseBlock.text(content));
            }
        });

        tokenStream.onPartialThinking(partial -> {
            if (StringUtils.isNotBlank(partial.text())) {
                agentLogService.record(
                        AgentLogType.TOKEN_PARTIAL_THINKING,
                        "ChatStreamBridge",
                        "partial_thinking",
                        AgentLogFields.of("length", partial.text().length(), "preview", partial.text()));
                sink.tryEmitNext(ChatResponseBlock.thought(partial.text()));
            }
        });

        tokenStream.onPartialToolCallWithContext((partialToolCall, context) -> {
            log.debug("Partial tool call: index={}, id={}, name={}, partialArgs='{}'",
                    partialToolCall.index(), partialToolCall.id(), partialToolCall.name(),
                    partialToolCall.partialArguments());

            if (Objects.nonNull(partialToolCall.id())) {
                streamedToolCallIds.add(partialToolCall.id());
                if (ToolNameEnum.isSubAgentTool(partialToolCall.name())) {
                    log.debug("SubAgent tool detected: setting parentToolCallId={}", partialToolCall.id());
                    AgentExecutionContext.setParentToolCallId(partialToolCall.id());
                }
            }
            agentLogService.record(
                    AgentLogType.TOKEN_TOOL_CALL,
                    "ChatStreamBridge",
                    "partial_tool_call",
                    AgentLogFields.of(
                            "toolCallId", partialToolCall.id(),
                            "toolName", partialToolCall.name(),
                            "arguments", partialToolCall.partialArguments(),
                            "streaming", true));

            sink.tryEmitNext(ChatResponseBlock.toolCall(
                    partialToolCall.id(),
                    partialToolCall.name(),
                    partialToolCall.partialArguments(),
                    true
            ));
        });

        tokenStream.onIntermediateResponse(response -> {
            if (!response.aiMessage().hasToolExecutionRequests()) {
                return;
            }
            for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                if (streamedToolCallIds.contains(toolRequest.id())) {
                    log.debug("Skipping already-streamed tool call: id={}, name={}",
                            toolRequest.id(), toolRequest.name());
                    continue;
                }

                log.debug("Complete tool call (non-streaming provider): id={}, name={}",
                        toolRequest.id(), toolRequest.name());

                if (ToolNameEnum.isSubAgentTool(toolRequest.name())) {
                    log.debug("SubAgent tool detected (non-streaming): setting parentToolCallId={}", toolRequest.id());
                    AgentExecutionContext.setParentToolCallId(toolRequest.id());
                }
                agentLogService.record(
                        AgentLogType.TOKEN_TOOL_CALL,
                        "ChatStreamBridge",
                        "tool_call",
                        AgentLogFields.of(
                                "toolCallId", toolRequest.id(),
                                "toolName", toolRequest.name(),
                                "arguments", toolRequest.arguments(),
                                "streaming", false));

                sink.tryEmitNext(ChatResponseBlock.toolCall(
                        toolRequest.id(),
                        toolRequest.name(),
                        toolRequest.arguments(),
                        false
                ));
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();
            toolTracker.record(req.name());
            log.info("Tool executed: name={}, toolCallId={}, failed={}, resultLen={}",
                    req.name(), req.id(), toolExecution.hasFailed(),
                    toolExecution.result() != null ? toolExecution.result().toString().length() : 0);

            if (ToolNameEnum.ENTER_PLAN_MODE.getToolName().equals(req.name())) {
                enterPlanTriggered.set(true);
            }

            if (ToolNameEnum.isSubAgentTool(req.name())) {
                AgentExecutionContext.clear();
            }
            agentLogService.record(
                    AgentLogType.TOKEN_TOOL_RESULT,
                    "ChatStreamBridge",
                    "tool_result",
                    AgentLogFields.of(
                            "toolCallId", req.id(),
                            "toolName", req.name(),
                            "failed", toolExecution.hasFailed(),
                            "resultLength", toolExecution.result() != null ? toolExecution.result().toString().length() : 0));

            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed()));
        });

        tokenStream.onCompleteResponse(response -> {
            AgentExecutionContext.clear();
            sseEmitterRegistry.unregister(conversationId);
            log.debug("[ChatStream] sink unregistered for conversation {}", conversationId);
            collectTokenUsage(response, toolTracker);
            log.info("Conversation {} completed: toolCount={}, outputTokens={}, totalTokens={}",
                    conversationId, toolTracker.getTotalCount(),
                    response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : null,
                    response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null);
            agentLogService.record(
                    AgentLogType.TOKEN_COMPLETE,
                    "ChatStreamBridge",
                    "token_stream_complete",
                    AgentLogFields.of(
                            "toolCount", toolTracker.getTotalCount(),
                            "outputTokens", response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : null,
                            "totalTokens", response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : null));
            publishTokenUsageIfPresent(response, conversationId);

            if (emitDoneBlock) {
                sink.tryEmitNext(ChatResponseBlock.doneBlock(toolTracker.toMetadata()));
            }
            sink.tryEmitComplete();
        });

        tokenStream.onError(error -> {
            AgentExecutionContext.clear();
            log.error("Error in chat stream", error);
            agentLogService.recordError(AgentLogType.TOKEN_ERROR, "ChatStreamBridge", "token_stream_error", error, null);
            sseEmitterRegistry.unregister(conversationId);
            log.debug("[ChatStream] sink unregistered for conversation {} (on error)", conversationId);
            sink.tryEmitError(error);
        });

        tokenStream.start();
        return sink.asFlux();
    }
    private void collectTokenUsage(
            dev.langchain4j.model.chat.response.ChatResponse response, AgentToolTracker toolTracker) {
        if (Objects.nonNull(response.tokenUsage())) {
            toolTracker.setTokenUsage(
                    response.tokenUsage().outputTokenCount(),
                    response.tokenUsage().totalTokenCount());
        }
    }

    private void publishTokenUsageIfPresent(
            dev.langchain4j.model.chat.response.ChatResponse response, Long conversationId) {
        if (Objects.isNull(response.tokenUsage())) {
            return;
        }
        Integer outputTokens = response.tokenUsage().outputTokenCount();
        Integer totalTokens = response.tokenUsage().totalTokenCount();

        if (Objects.nonNull(totalTokens) && totalTokens > 0) {
            log.info("Chat completed for conversation {}: {} total tokens (output: {})",
                    conversationId, totalTokens, outputTokens);
            eventPublisher.publishEvent(
                    new ChatCompletedEvent(this, conversationId, outputTokens, totalTokens));
        } else {
            log.debug("No token usage available for conversation {}", conversationId);
        }
    }
}
