package edu.zsc.ai.observability.decorator;

import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class LoggingExecutorServiceDecorator extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final AgentLogService agentLogService;
    private final String loggerName;

    public LoggingExecutorServiceDecorator(ExecutorService delegate, AgentLogService agentLogService, String loggerName) {
        this.delegate = delegate;
        this.agentLogService = agentLogService;
        this.loggerName = loggerName;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        RequestContextInfo requestContext = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContext = AgentRequestContext.snapshot();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        String taskId = AgentExecutionContext.getTaskId();
        delegate.execute(() -> {
            RequestContextInfo previousRequest = RequestContext.snapshot();
            AgentRequestContextInfo previousAgentRequest = AgentRequestContext.snapshot();
            String previousParentToolCallId = AgentExecutionContext.getParentToolCallId();
            String previousTaskId = AgentExecutionContext.getTaskId();
            restoreContexts(requestContext, agentRequestContext, parentToolCallId, taskId);
            long startTime = System.currentTimeMillis();
            agentLogService.record(AgentLogEvent.builder()
                    .timestamp(Instant.now())
                    .type(AgentLogType.EXECUTOR_TASK_START)
                    .loggerName(loggerName)
                    .conversationId(requestContext != null ? requestContext.getConversationId() : null)
                    .taskId(taskId)
                    .parentToolCallId(parentToolCallId)
                    .message("executor_task_start")
                    .payload(AgentLogFields.of("executor", loggerName))
                    .build());
            try {
                command.run();
                agentLogService.record(AgentLogEvent.builder()
                        .timestamp(Instant.now())
                        .type(AgentLogType.EXECUTOR_TASK_COMPLETE)
                        .loggerName(loggerName)
                        .conversationId(requestContext != null ? requestContext.getConversationId() : null)
                        .taskId(taskId)
                        .parentToolCallId(parentToolCallId)
                        .elapsedMs(System.currentTimeMillis() - startTime)
                        .message("executor_task_complete")
                        .payload(AgentLogFields.of("executor", loggerName))
                        .build());
            } catch (Exception ex) {
                agentLogService.recordError(
                        AgentLogType.EXECUTOR_TASK_ERROR,
                        loggerName,
                        "executor_task_error",
                        ex,
                        AgentLogFields.of("executor", loggerName, "elapsedMs", System.currentTimeMillis() - startTime));
                throw ex;
            } finally {
                restoreContexts(previousRequest, previousAgentRequest, previousParentToolCallId, previousTaskId);
            }
        });
    }

    private void restoreContexts(RequestContextInfo requestContext,
                                 AgentRequestContextInfo agentRequestContext,
                                 String parentToolCallId,
                                 String taskId) {
        if (requestContext != null) {
            RequestContext.set(requestContext);
        } else {
            RequestContext.clear();
        }
        if (agentRequestContext != null) {
            AgentRequestContext.set(agentRequestContext);
        } else {
            AgentRequestContext.clear();
        }
        AgentExecutionContext.setParentToolCallId(parentToolCallId);
        AgentExecutionContext.setTaskId(taskId);
    }
}
