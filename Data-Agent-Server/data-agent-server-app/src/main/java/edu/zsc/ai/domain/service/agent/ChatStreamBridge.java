package edu.zsc.ai.domain.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.domain.event.ChatCompletedEvent;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
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

    /**
     * Start the agent chat from the given session and bridge the resulting
     * {@link TokenStream} into a {@link Flux} of {@link ChatResponseBlock}.
     *
     * @param session            the prepared chat session (agent + parameters)
     * @param enterPlanTriggered set to {@code true} when the enterPlanMode tool fires
     * @param emitDoneBlock      whether to emit a done-block when the stream completes
     */
    public Flux<ChatResponseBlock> bridge(
            ChatSession session,
            AtomicBoolean enterPlanTriggered,
            boolean emitDoneBlock) {

        Long conversationId = session.conversationId();
        Long runId = session.contextSnapshot().getRunId();
        Long taskId = session.contextSnapshot().getTaskId();
        String agentRole = session.contextSnapshot().getAgentRole();
        TokenStream tokenStream = session.startChat();

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        sseEmitterRegistry.register(conversationId, sink);
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
                    true,
                    runId,
                    taskId,
                    agentRole
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

                sink.tryEmitNext(ChatResponseBlock.toolCall(
                        toolRequest.id(),
                        toolRequest.name(),
                        toolRequest.arguments(),
                        false,
                        runId,
                        taskId,
                        agentRole
                ));
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();

            if (ToolNameEnum.ENTER_PLAN_MODE.getToolName().equals(req.name())) {
                enterPlanTriggered.set(true);
            }

            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed(),
                    runId,
                    taskId,
                    agentRole));
        });

        tokenStream.onCompleteResponse(response -> {
            sseEmitterRegistry.unregister(conversationId);
            publishTokenUsageIfPresent(response, conversationId);

            if (emitDoneBlock) {
                sink.tryEmitNext(ChatResponseBlock.doneBlock());
            }
            sink.tryEmitComplete();
        });

        tokenStream.onError(error -> {
            log.error("Error in chat stream", error);
            sseEmitterRegistry.unregister(conversationId);
            sink.tryEmitError(error);
        });

        tokenStream.start();
        return sink.asFlux();
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
