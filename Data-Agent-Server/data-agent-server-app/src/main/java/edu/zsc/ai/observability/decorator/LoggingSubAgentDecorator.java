package edu.zsc.ai.observability.decorator;

import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class LoggingSubAgentDecorator<I, O> implements SubAgent<I, O> {

    private final String loggerName;
    private final SubAgent<I, O> delegate;
    private final AgentLogService agentLogService;
    private final Function<I, Map<String, Object>> payloadFactory;
    private final Function<I, Long> timeoutExtractor;

    @Override
    public AgentTypeEnum getAgentType() {
        return delegate.getAgentType();
    }

    @Override
    public O invoke(I request) {
        long startTime = System.currentTimeMillis();
        agentLogService.record(AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(AgentLogType.SUB_AGENT_START)
                .loggerName(loggerName)
                .conversationId(RequestContext.snapshot() != null ? RequestContext.snapshot().getConversationId() : null)
                .agentType(delegate.getAgentType().getCode())
                .taskId(AgentExecutionContext.getTaskId())
                .parentToolCallId(AgentExecutionContext.getParentToolCallId())
                .message("sub_agent_start")
                .payload(payloadFactory.apply(request))
                .build());
        try {
            O result = delegate.invoke(request);
            agentLogService.record(AgentLogEvent.builder()
                    .timestamp(Instant.now())
                    .type(AgentLogType.SUB_AGENT_COMPLETE)
                    .loggerName(loggerName)
                    .conversationId(RequestContext.snapshot() != null ? RequestContext.snapshot().getConversationId() : null)
                    .agentType(delegate.getAgentType().getCode())
                    .taskId(AgentExecutionContext.getTaskId())
                    .parentToolCallId(AgentExecutionContext.getParentToolCallId())
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .message("sub_agent_complete")
                    .payload(payloadFactory.apply(request))
                    .status("success")
                    .build());
            return result;
        } catch (Exception ex) {
            AgentLogType errorType = isTimeout(ex) ? AgentLogType.SUB_AGENT_TIMEOUT : AgentLogType.SUB_AGENT_ERROR;
            Map<String, Object> payload = payloadFactory.apply(request);
            Long timeoutSeconds = timeoutExtractor.apply(request);
            if (timeoutSeconds != null) {
                payload.put("timeoutSeconds", timeoutSeconds);
            }
            agentLogService.record(AgentLogEvent.builder()
                    .timestamp(Instant.now())
                    .type(errorType)
                    .loggerName(loggerName)
                    .conversationId(RequestContext.snapshot() != null ? RequestContext.snapshot().getConversationId() : null)
                    .agentType(delegate.getAgentType().getCode())
                    .taskId(AgentExecutionContext.getTaskId())
                    .parentToolCallId(AgentExecutionContext.getParentToolCallId())
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .message("sub_agent_error")
                    .payload(payload)
                    .status("error")
                    .errorClass(ex.getClass().getName())
                    .errorMessage(ex.getMessage())
                    .build());
            throw ex;
        }
    }

    public static LoggingSubAgentDecorator<SubAgentRequest, ?> explorer(SubAgent<SubAgentRequest, ?> delegate, AgentLogService agentLogService) {
        Assert.notNull(delegate, "delegate must not be null");
        return new LoggingSubAgentDecorator<>(
                "ExplorerSubAgent",
                delegate,
                agentLogService,
                request -> AgentLogFields.of(
                        "instructionPreview", request != null ? request.instruction() : null,
                        "contextPreview", request != null ? request.context() : null,
                        "connectionIds", request != null ? request.connectionIds() : null),
                request -> request != null ? request.timeoutSeconds() : null
        );
    }

    public static LoggingSubAgentDecorator<PlannerRequest, ?> planner(SubAgent<PlannerRequest, ?> delegate, AgentLogService agentLogService) {
        Assert.notNull(delegate, "delegate must not be null");
        return new LoggingSubAgentDecorator<>(
                "PlannerSubAgent",
                delegate,
                agentLogService,
                request -> AgentLogFields.of(
                        "instructionPreview", request != null ? request.getInstruction() : null,
                        "hasSchemaSummary", request != null && request.getSchemaSummary() != null),
                request -> request != null ? request.getTimeoutSeconds() : null
        );
    }

    private boolean isTimeout(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
