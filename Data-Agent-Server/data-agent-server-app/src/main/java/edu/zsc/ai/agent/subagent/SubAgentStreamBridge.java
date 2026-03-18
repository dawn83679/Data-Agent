package edu.zsc.ai.agent.subagent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Bridges a SubAgent's TokenStream to the main SSE sink so all SubAgent output
 * (tool calls, tool results, text) reaches the frontend.
 * <p>
 * Emits blocks with parentToolCallId so the frontend can nest them under the callingSubAgent card.
 */
@Slf4j
@Component
public class SubAgentStreamBridge {

    private final AgentLogService agentLogService;

    public SubAgentStreamBridge(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    /**
     * Attach bridge handlers to the SubAgent's TokenStream and emit to the sink.
     * Call this before tokenStream.start(). The stream will be consumed; also wire
     * onPartialResponse/onCompleteResponse for your own parsing/accumulation.
     *
     * @param tokenStream   the SubAgent's chat TokenStream
     * @param sink          the SSE sink (from SseEmitterRegistry)
     * @param parentToolCallId id of the callingSubAgent tool call (for nesting)
     */
    public void bridge(TokenStream tokenStream, Sinks.Many<ChatResponseBlock> sink, String parentToolCallId) {
        bridge(tokenStream, sink, parentToolCallId, null, null);
    }

    /**
     * Same as {@link #bridge(TokenStream, Sinks.Many, String)} but with onToolExecutedCallback.
     */
    public void bridge(TokenStream tokenStream, Sinks.Many<ChatResponseBlock> sink, String parentToolCallId,
                       BiConsumer<ToolExecutionRequest, Object> onToolExecutedCallback) {
        bridge(tokenStream, sink, parentToolCallId, onToolExecutedCallback, null);
    }

    /**
     * Same as {@link #bridge(TokenStream, Sinks.Many, String)} but with optional callbacks.
     * onToolExecutedCallback: (toolRequest, rawResult) — e.g. Explorer collects getObjectDetail results.
     * onPartialResponseCallback: accumulates LLM text — TokenStream allows onPartialResponse only once.
     */
    public void bridge(TokenStream tokenStream, Sinks.Many<ChatResponseBlock> sink, String parentToolCallId,
                       BiConsumer<ToolExecutionRequest, Object> onToolExecutedCallback,
                       Consumer<String> onPartialResponseCallback) {
        if (sink == null && onToolExecutedCallback == null && onPartialResponseCallback == null) return;
        String parentId = StringUtils.isNotBlank(parentToolCallId) ? parentToolCallId : null;
        String taskId = AgentExecutionContext.getTaskId();
        Set<String> streamedToolCallIds = new HashSet<>();
        AtomicBoolean firstPartial = new AtomicBoolean(false);

        tokenStream.onPartialToolCallWithContext((partialToolCall, context) -> {
            if (partialToolCall == null) return;
            String id = partialToolCall.id();
            if (id != null && !id.isEmpty()) {
                streamedToolCallIds.add(id);
                log.debug("SubAgent tool call started: toolCallId={}, name={}, parentToolCallId={}", id, partialToolCall.name(), parentId);
            }
            String args = partialToolCall.partialArguments();
            agentLogService.record(
                    AgentLogType.TOKEN_TOOL_CALL,
                    "SubAgentStreamBridge",
                    "partial_tool_call",
                    AgentLogFields.of(
                            "toolCallId", id,
                            "toolName", partialToolCall.name(),
                            "arguments", args,
                            "streaming", true,
                            "parentToolCallId", parentId,
                            "taskId", taskId));
            ChatResponseBlock block = ChatResponseBlock.toolCall(
                    id, partialToolCall.name(), args != null ? args : "", true);
            block.setParentToolCallId(parentId);
            block.setSubAgentTaskId(taskId);
            if (sink != null) {
                Sinks.EmitResult emitResult = sink.tryEmitNext(block);
                if (emitResult.isFailure()) {
                    log.warn("[StreamBridge] sink emission failed for {}", id != null ? id : "partialToolCall");
                }
            }
        });

        tokenStream.onIntermediateResponse(response -> {
            if (response == null || !response.aiMessage().hasToolExecutionRequests()) return;
            log.debug("[StreamBridge] intermediate response: {} tool requests", response.aiMessage().toolExecutionRequests().size());
            for (ToolExecutionRequest req : response.aiMessage().toolExecutionRequests()) {
                if (streamedToolCallIds.contains(req.id())) continue;
                agentLogService.record(
                        AgentLogType.TOKEN_TOOL_CALL,
                        "SubAgentStreamBridge",
                        "tool_call",
                        AgentLogFields.of(
                                "toolCallId", req.id(),
                                "toolName", req.name(),
                                "arguments", req.arguments(),
                                "streaming", false,
                                "parentToolCallId", parentId,
                                "taskId", taskId));
                ChatResponseBlock block = ChatResponseBlock.toolCall(
                        req.id(), req.name(), req.arguments() != null ? req.arguments() : "", false);
                block.setParentToolCallId(parentId);
                block.setSubAgentTaskId(taskId);
                if (sink != null) sink.tryEmitNext(block);
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();
            Object rawResult = toolExecution.result();
            String result = rawResult != null ? rawResult.toString() : "";
            log.info("SubAgent tool executed: name={}, toolCallId={}, failed={}, resultLen={}",
                    req.name(), req.id(), toolExecution.hasFailed(),
                    result.length());
            if (onToolExecutedCallback != null) {
                onToolExecutedCallback.accept(req, rawResult);
            }
            agentLogService.record(
                    AgentLogType.TOKEN_TOOL_RESULT,
                    "SubAgentStreamBridge",
                    "tool_result",
                    AgentLogFields.of(
                            "toolCallId", req.id(),
                            "toolName", req.name(),
                            "failed", toolExecution.hasFailed(),
                            "resultLength", result.length(),
                            "parentToolCallId", parentId,
                            "taskId", taskId));
            ChatResponseBlock block = ChatResponseBlock.toolResult(
                    req.id(), req.name(), result, toolExecution.hasFailed());
            block.setParentToolCallId(parentId);
            block.setSubAgentTaskId(taskId);
            if (sink != null) sink.tryEmitNext(block);
        });

        tokenStream.onPartialResponse(content -> {
            if (firstPartial.compareAndSet(false, true)) {
                log.debug("SubAgent started emitting text response, parentToolCallId={}", parentId);
            }
            agentLogService.record(
                    AgentLogType.TOKEN_PARTIAL_RESPONSE,
                    "SubAgentStreamBridge",
                    "partial_response",
                    AgentLogFields.of(
                            "parentToolCallId", parentId,
                            "taskId", taskId,
                            "length", content != null ? content.length() : 0,
                            "preview", content));
            if (onPartialResponseCallback != null) onPartialResponseCallback.accept(content);
            // Do NOT emit SubAgent text to the main SSE sink here.
            // SubAgent LLM text is accumulated via onPartialResponseCallback and delivered
            // to the frontend as part of the parent TOOL_RESULT block. Emitting it directly
            // would cause it to appear as top-level TEXT blocks in the main chat stream.
        });
    }
}
