package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.agent.tool.AgentToolTracker;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * SubAgent observability: trace tracking, SSE progress events, tool execution monitoring.
 * <p>
 * Called explicitly by SubAgent implementations (emitStart/emitComplete/emitError)
 * instead of via AgentListener callbacks, because AiServices.builder() does not
 * support the AgentListener interface (which requires LangChain4jManaged ThreadLocal).
 */
@Slf4j
public class SubAgentObservabilityListener {

    private final AgentTypeEnum agentType;
    private final Long conversationId;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final BiConsumer<String, Object> onToolExecutedCallback;
    private final String taskId;
    private final String parentToolCallId;
    private final Long connectionId;
    private final Long timeoutSeconds;
    private final AgentLogService agentLogService;

    // Tool usage tracking
    private final AgentToolTracker toolTracker = new AgentToolTracker();

    // Trace state
    private final String traceId;
    private final List<SpanRecord> spans = new CopyOnWriteArrayList<>();
    private final AtomicInteger cumulativeTokens = new AtomicInteger(0);
    private final AtomicInteger spanCounter = new AtomicInteger(0);

    // Current invocation tracking
    private long invocationStartMs;

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry) {
        this(agentType, conversationId, sseEmitterRegistry, null, null, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback) {
        this(agentType, conversationId, sseEmitterRegistry, onToolExecutedCallback, null, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         String taskId) {
        this(agentType, conversationId, sseEmitterRegistry, null, taskId, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback,
                                         String taskId) {
        this(agentType, conversationId, sseEmitterRegistry, onToolExecutedCallback, taskId, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback,
                                         String taskId,
                                         String parentToolCallId) {
        this(agentType, conversationId, sseEmitterRegistry, onToolExecutedCallback, taskId, parentToolCallId, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback,
                                         String taskId,
                                         String parentToolCallId,
                                         Long connectionId) {
        this(agentType, conversationId, sseEmitterRegistry, onToolExecutedCallback, taskId, parentToolCallId, connectionId, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback,
                                         String taskId,
                                         String parentToolCallId,
                                         Long connectionId,
                                         Long timeoutSeconds) {
        this(agentType, conversationId, sseEmitterRegistry, onToolExecutedCallback, taskId, parentToolCallId, connectionId, timeoutSeconds, null);
    }

    public SubAgentObservabilityListener(AgentTypeEnum agentType,
                                         Long conversationId,
                                         SseEmitterRegistry sseEmitterRegistry,
                                         BiConsumer<String, Object> onToolExecutedCallback,
                                         String taskId,
                                         String parentToolCallId,
                                         Long connectionId,
                                         Long timeoutSeconds,
                                         AgentLogService agentLogService) {
        this.agentType = agentType;
        this.conversationId = conversationId;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.onToolExecutedCallback = onToolExecutedCallback;
        this.taskId = taskId;
        this.parentToolCallId = parentToolCallId;
        this.connectionId = connectionId;
        this.timeoutSeconds = timeoutSeconds;
        this.agentLogService = agentLogService;
        this.traceId = "conv-" + conversationId + "-" + System.currentTimeMillis();
    }

    /**
     * Call at the start of SubAgent invocation.
     */
    public void emitStart() {
        invocationStartMs = System.currentTimeMillis();
        emitSubAgentBlock(ChatResponseBlock.subAgentStart(agentType.getCode(), parentToolCallId, taskId, connectionId, timeoutSeconds));
        recordLifecycleEvent(AgentLogType.SUB_AGENT_START, "sub_agent_start", null, null);
        log.debug("SubAgent [{}] invocation started, traceId={}", agentType.getCode(), traceId);
    }

    /**
     * Call when SubAgent invocation completes successfully.
     */
    public void emitComplete() {
        emitComplete(null, null);
    }

    public void emitComplete(String summaryText, String resultJson) {
        long durationMs = System.currentTimeMillis() - invocationStartMs;
        int seq = spanCounter.incrementAndGet();

        SpanRecord span = new SpanRecord(
                traceId,
                agentType.getCode() + "-" + seq,
                agentType,
                invocationStartMs,
                System.currentTimeMillis(),
                durationMs,
                0,
                true,
                null
        );
        spans.add(span);

        emitSubAgentBlock(ChatResponseBlock.subAgentComplete(
                agentType.getCode(), parentToolCallId, taskId,
                toolTracker.getTotalCount(), toolTracker.getToolCounts(), summaryText, resultJson, connectionId, timeoutSeconds));
        recordLifecycleEvent(AgentLogType.SUB_AGENT_COMPLETE, "sub_agent_complete", null, AgentLogFields.of(
                "toolCount", toolTracker.getTotalCount(),
                "toolCounts", toolTracker.getToolCounts(),
                "summaryText", summaryText));
        log.info("SubAgent [{}] completed: duration={}ms, {} tool calls, traceId={}, spanId={}",
                agentType.getCode(), durationMs, toolTracker.getTotalCount(), traceId, span.spanId());
    }

    /**
     * Call when SubAgent invocation fails.
     */
    public void emitError(String errorMessage) {
        long durationMs = System.currentTimeMillis() - invocationStartMs;
        int seq = spanCounter.incrementAndGet();
        String errorMsg = errorMessage != null ? errorMessage : "unknown error";

        SpanRecord span = new SpanRecord(
                traceId,
                agentType.getCode() + "-" + seq,
                agentType,
                invocationStartMs,
                System.currentTimeMillis(),
                durationMs,
                0,
                false,
                errorMsg
        );
        spans.add(span);

        emitSubAgentBlock(ChatResponseBlock.subAgentError(agentType.getCode(), errorMsg, parentToolCallId, taskId, connectionId, timeoutSeconds));
        recordLifecycleEvent(AgentLogType.SUB_AGENT_ERROR, "sub_agent_error", null, AgentLogFields.of("errorMessage", errorMsg));
        log.error("SubAgent [{}] failed: duration={}ms, error={}, traceId={}",
                agentType.getCode(), durationMs, errorMsg, traceId);
    }

    /**
     * Record a tool execution (called from SubAgentStreamBridge or tool result collectors).
     */
    public void recordToolExecution(String toolName, Object result) {
        if (toolName != null) {
            toolTracker.record(toolName);
            log.debug("SubAgent [{}] tool executed: name={}, hasCallback={}",
                    agentType.getCode(), toolName, onToolExecutedCallback != null);
        }
        if (onToolExecutedCallback != null && toolName != null) {
            try {
                onToolExecutedCallback.accept(toolName, result);
            } catch (Exception e) {
                log.warn("[Observability] tool callback failed", e);
            }
        }
    }

    /**
     * Build trace metadata map for inclusion in tool results.
     */
    public Map<String, Object> toTraceMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("traceId", traceId);
        metadata.put("cumulativeTokens", cumulativeTokens.get());
        metadata.put("spanCount", spans.size());
        metadata.put("spans", spans.stream().map(SpanRecord::toMap).toList());
        return metadata;
    }

    public String getTraceId() {
        return traceId;
    }

    public void addTokenUsage(int tokens) {
        cumulativeTokens.addAndGet(tokens);
    }

    public int getCumulativeTokens() {
        return cumulativeTokens.get();
    }

    private void emitSubAgentBlock(ChatResponseBlock block) {
        if (sseEmitterRegistry == null) return;
        log.debug("[Observability] emitting {}: agentType={}", block.getType(), agentType.getCode());
        sseEmitterRegistry.get(conversationId).ifPresent(sink -> sink.tryEmitNext(block));
    }

    private void recordLifecycleEvent(AgentLogType type, String message, Throwable throwable, Map<String, Object> payload) {
        if (agentLogService == null) {
            return;
        }
        agentLogService.record(AgentLogEvent.builder()
                .type(type)
                .loggerName("SubAgentObservabilityListener")
                .conversationId(conversationId)
                .traceId(traceId)
                .agentType(agentType.getCode())
                .taskId(taskId)
                .parentToolCallId(parentToolCallId)
                .elapsedMs(invocationStartMs > 0 ? System.currentTimeMillis() - invocationStartMs : null)
                .message(message)
                .payload(payload)
                .errorClass(throwable != null ? throwable.getClass().getName() : null)
                .errorMessage(throwable != null ? throwable.getMessage() : null)
                .build());
    }

    /**
     * Internal span record replacing SubAgentSpan.
     */
    public record SpanRecord(
            String traceId,
            String spanId,
            AgentTypeEnum agentType,
            long startTimeMs,
            long endTimeMs,
            long durationMs,
            int tokenUsage,
            boolean success,
            String errorMessage
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("spanId", spanId);
            map.put("agentType", agentType.getCode());
            map.put("durationMs", durationMs);
            map.put("tokenUsage", tokenUsage);
            map.put("success", success);
            if (errorMessage != null) {
                map.put("error", errorMessage);
            }
            return map;
        }
    }
}
