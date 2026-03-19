package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.SubAgentTimeoutPolicy;
import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.ExplorerSubAgentExecutorConfig;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.*;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Delegates schema exploration to Explorer SubAgent(s).
 * Accepts a list of ExplorerTask — each task spawns one Explorer SubAgent.
 * Multiple tasks run concurrently; results are returned task-by-task to MainAgent.
 */
@AgentTool
@Slf4j
public class CallingExplorerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;
    private final Executor explorerExecutor;
    private final AgentLogService agentLogService;

    public CallingExplorerTool(
            SubAgentManager subAgentManager,
            @Qualifier(ExplorerSubAgentExecutorConfig.EXPLORER_SUB_AGENT_EXECUTOR_BEAN) Executor explorerExecutor,
            AgentLogService agentLogService) {
        this.subAgentManager = subAgentManager;
        this.explorerExecutor = explorerExecutor;
        this.agentLogService = agentLogService;
    }

    @Tool({
            "Value: delegates schema exploration to one or more Explorer sub-agents and returns structured findings you can plan against.",
            "Use When: call when you need verified schema context before generating SQL, or when discovery spans multiple candidate connections or scopes.",
            "Preconditions: each task needs connectionId and instruction. Use connectionId from getEnvironmentOverview. Each task may include context and timeoutSeconds. Top-level timeoutSeconds applies only to tasks that do not set their own timeout.",
            "After Success: review taskResults, keep the objects and summaries that match the user goal, and then call callingPlannerSubAgent or askUserQuestion if ambiguity remains.",
            "After Partial Success: continue only with successful taskResults. Do not assume failed tasks found nothing; retry or ask the user before dropping those scopes.",
            "After Failure: narrow the task scope, correct the connectionId or instruction, or retry later. Do not proceed to SQL planning without usable explorer output.",
            "Result Consumption: each taskResult includes taskId, summaryText, objects, and rawResponse. Consume them task by task instead of blindly merging all tasks.",
            "Relation: usually after getEnvironmentOverview or focused discovery and before callingPlannerSubAgent. Multiple tasks run concurrently. Explorer timeout defaults to 120 seconds, and lower values are raised to 120."
    })
    public AgentToolResult callingExplorerSubAgent(
            @P("Explorer task list. Each item: {connectionId: number, instruction: string, context?: string, timeoutSeconds?: number}. timeoutSeconds uses seconds and values below 120 are automatically raised to 120.") List<ExplorerTask> tasks,
            @P(value = "Optional default timeout in seconds for explorer tasks that do not provide their own timeoutSeconds. Default is 120 seconds. Values below 120 are automatically raised to 120.", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        if (CollectionUtils.isEmpty(tasks)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasks is required. Provide a list of {connectionId, instruction, context?}."
            );
        }
        validateTasks(tasks);

        long effectiveDefaultTimeoutSeconds = resolveTimeoutSeconds(timeoutSeconds, subAgentManager.getProperties().getExplorer().getTimeoutSeconds());
        log.info("[Tool] callingExplorerSubAgent start, conversationId={}, parentToolCallId={}, taskCount={}, requestedTimeoutSeconds={}, effectiveDefaultTimeoutSeconds={}, tasks={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                AgentExecutionContext.getParentToolCallId(),
                tasks.size(),
                timeoutSeconds,
                effectiveDefaultTimeoutSeconds,
                summarizeTasks(tasks));
        agentLogService.recordDebug("CallingExplorerTool", "tool_start", AgentLogFields.of(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", AgentExecutionContext.getParentToolCallId(),
                "taskCount", tasks.size(),
                "requestedTimeoutSeconds", timeoutSeconds,
                "effectiveDefaultTimeoutSeconds", effectiveDefaultTimeoutSeconds,
                "minimumTimeoutSeconds", SubAgentTimeoutPolicy.MIN_TIMEOUT_SECONDS,
                "tasks", summarizeTasks(tasks)
        ));

        if (tasks.size() == 1) {
            return invokeSingle(tasks.get(0), timeoutSeconds);
        }

        return invokeConcurrent(tasks, timeoutSeconds);
    }

    private AgentToolResult invokeSingle(ExplorerTask task, Long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long effectiveTimeoutSeconds = resolveTaskTimeoutSeconds(task, timeoutSeconds);
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        ExplorerTaskResult result = executeTask(
                task,
                effectiveTimeoutSeconds,
                requestContextSnapshot,
                agentRequestContextSnapshot,
                AgentExecutionContext.getParentToolCallId(),
                buildTaskId("explore", requestContextSnapshot)
        );
        int objectCount = CollectionUtils.size(result.getObjects());
        log.info("[Tool done] callingExplorerSubAgent single, taskId={}, connectionId={}, status={}, objectCount={}, summaryLength={}, rawResponseLength={}, elapsedMs={}",
                result.getTaskId(),
                task.getConnectionId(),
                result.getStatus(),
                objectCount,
                StringUtils.length(result.getSummaryText()),
                StringUtils.length(result.getRawResponse()),
                System.currentTimeMillis() - startTime);
        agentLogService.recordDebug("CallingExplorerTool", "tool_done_single", AgentLogFields.of(
                "taskId", result.getTaskId(),
                "connectionId", task.getConnectionId(),
                "status", result.getStatus(),
                "objectCount", objectCount,
                "summaryLength", StringUtils.length(result.getSummaryText()),
                "rawResponseLength", StringUtils.length(result.getRawResponse()),
                "elapsedMs", System.currentTimeMillis() - startTime
        ));
        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(result))
                .build();
        return buildExplorerAgentResult(List.of(task), envelope);
    }

    private AgentToolResult invokeConcurrent(List<ExplorerTask> tasks, Long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        long timeout = resolveConcurrentTimeoutSeconds(tasks, timeoutSeconds);

        log.info("[Tool] callingExplorerSubAgent concurrent dispatch, parentToolCallId={}, effectiveTimeoutSeconds={}, taskCount={}",
                parentToolCallId,
                timeout,
                tasks.size());
        agentLogService.recordDebug("CallingExplorerTool", "concurrent_dispatch", AgentLogFields.of(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", parentToolCallId,
                "effectiveTimeoutSeconds", timeout,
                "taskCount", tasks.size()
        ));

        List<CompletableFuture<ExplorerTaskResult>> futures = tasks.stream().map(explorerTask -> {
                    String taskId = buildTaskId("explore", requestContextSnapshot);
                    return CompletableFuture.supplyAsync(() -> executeTask(
                            explorerTask, resolveTaskTimeoutSeconds(explorerTask, timeoutSeconds), requestContextSnapshot, agentRequestContextSnapshot, parentToolCallId, taskId), explorerExecutor);
                })
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeout + 30, TimeUnit.SECONDS); // extra 30s grace for orchestration overhead

            List<ExplorerTaskResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            long successCount = results.stream().filter(result -> result.getStatus() == ExplorerTaskStatus.SUCCESS).count();
            long errorCount = results.size() - successCount;
            log.info("[Tool done] callingExplorerSubAgent concurrent, successCount={}, errorCount={}, elapsedMs={}, resultSummary={}",
                    successCount,
                    errorCount,
                    System.currentTimeMillis() - startTime,
                    summarizeTaskResults(results));
            agentLogService.recordDebug("CallingExplorerTool", "tool_done_concurrent", AgentLogFields.of(
                    "successCount", successCount,
                    "errorCount", errorCount,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "resultSummary", summarizeTaskResults(results)
            ));
            ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                    .taskResults(results)
                    .build();
            return buildExplorerAgentResult(tasks, envelope);

        } catch (TimeoutException e) {
            log.error("[Tool] callingExplorerSubAgent concurrent timeout, timeoutSeconds={}, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    timeout,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e));
            agentLogService.recordDebug("CallingExplorerTool", "concurrent_timeout", AgentLogFields.of(
                    "timeoutSeconds", timeout,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e)
            ));
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "Explorer timed out after " + timeout + "s",
                    "Concurrent Explorer timed out after " + timeout + "s",
                    true,
                    e
            );
        } catch (Exception e) {
            log.error("[Tool] callingExplorerSubAgent concurrent failed, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    e);
            agentLogService.recordDebug("CallingExplorerTool", "concurrent_failed", AgentLogFields.of(
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e)
            ));
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "Concurrent Explorer failed: " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()),
                    "Concurrent Explorer orchestration failed",
                    true,
                    e
            );
        }
    }

    private long resolveTaskTimeoutSeconds(ExplorerTask task, Long defaultTimeoutSeconds) {
        Long taskTimeoutSeconds = task != null ? task.getTimeoutSeconds() : null;
        return resolveTimeoutSeconds(taskTimeoutSeconds,
                resolveTimeoutSeconds(defaultTimeoutSeconds, subAgentManager.getProperties().getExplorer().getTimeoutSeconds()));
    }

    private long resolveConcurrentTimeoutSeconds(List<ExplorerTask> tasks, Long defaultTimeoutSeconds) {
        return tasks.stream()
                .mapToLong(task -> resolveTaskTimeoutSeconds(task, defaultTimeoutSeconds))
                .max()
                .orElse(subAgentManager.getProperties().getExplorer().getTimeoutSeconds());
    }

    private void validateTasks(List<ExplorerTask> tasks) {
        for (int index = 0; index < tasks.size(); index++) {
            ExplorerTask task = tasks.get(index);
            if (task == null) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "] is required."
                );
            }
            if (task.getConnectionId() == null || task.getConnectionId() <= 0) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].connectionId must be a positive number."
                );
            }
            if (StringUtils.isBlank(task.getInstruction())) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].instruction is required."
                );
            }
            if (task.getTimeoutSeconds() != null && task.getTimeoutSeconds() <= 0) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].timeoutSeconds must be a positive number."
                );
            }
        }
    }

    private ExplorerTaskResult executeTask(ExplorerTask task, Long timeoutSeconds,
                                           RequestContextInfo requestContextSnapshot,
                                           AgentRequestContextInfo agentRequestContextSnapshot,
                                           String parentToolCallId, String taskId) {
        long startTime = System.currentTimeMillis();
        log.info("[Explorer] task start, taskId={}, parentToolCallId={}, conversationId={}, connectionId={}, requestedTaskTimeoutSeconds={}, effectiveTimeoutSeconds={}, instructionLength={}, contextLength={}, instructionPreview={}, contextPreview={}",
                taskId,
                parentToolCallId,
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                task.getConnectionId(),
                task.getTimeoutSeconds(),
                timeoutSeconds,
                StringUtils.length(task.getInstruction()),
                StringUtils.length(task.getContext()),
                preview(task.getInstruction()),
                preview(task.getContext()));
        agentLogService.recordDebug("CallingExplorerTool", "task_start", AgentLogFields.of(
                "taskId", taskId,
                "parentToolCallId", parentToolCallId,
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "connectionId", task.getConnectionId(),
                "requestedTaskTimeoutSeconds", task.getTimeoutSeconds(),
                "effectiveTimeoutSeconds", timeoutSeconds,
                "instructionPreview", preview(task.getInstruction()),
                "contextPreview", preview(task.getContext())
        ));
        RequestContextInfo previousRequestContext = RequestContext.snapshot();
        AgentRequestContextInfo previousAgentRequestContext = AgentRequestContext.snapshot();
        if (requestContextSnapshot != null) {
            RequestContext.set(requestContextSnapshot);
        } else {
            RequestContext.clear();
        }
        if (agentRequestContextSnapshot != null) {
            AgentRequestContext.set(agentRequestContextSnapshot);
        } else {
            AgentRequestContext.clear();
        }
        String previousParentToolCallId = AgentExecutionContext.getParentToolCallId();
        String previousTaskId = AgentExecutionContext.getTaskId();
        AgentExecutionContext.setParentToolCallId(parentToolCallId);
        AgentExecutionContext.setTaskId(taskId);
        try {
            SubAgentRequest request = new SubAgentRequest(
                    task.getInstruction(),
                    List.of(task.getConnectionId()),
                    task.getContext(),
                    timeoutSeconds);
            SchemaSummary summary = subAgentManager.getExplorerSubAgent().invoke(request);
            log.info("[Explorer] task success, taskId={}, connectionId={}, objectCount={}, summaryLength={}, rawResponseLength={}, summaryPreview={}, elapsedMs={}",
                    taskId,
                    task.getConnectionId(),
                    summary != null ? CollectionUtils.size(summary.getObjects()) : 0,
                    summary != null ? StringUtils.length(summary.getSummaryText()) : 0,
                    summary != null ? StringUtils.length(summary.getRawResponse()) : 0,
                    summary != null ? preview(summary.getSummaryText()) : null,
                    System.currentTimeMillis() - startTime);
            agentLogService.recordDebug("CallingExplorerTool", "task_success", AgentLogFields.of(
                    "taskId", taskId,
                    "connectionId", task.getConnectionId(),
                    "objectCount", summary != null ? CollectionUtils.size(summary.getObjects()) : 0,
                    "summaryLength", summary != null ? StringUtils.length(summary.getSummaryText()) : 0,
                    "rawResponseLength", summary != null ? StringUtils.length(summary.getRawResponse()) : 0,
                    "summaryPreview", summary != null ? preview(summary.getSummaryText()) : null,
                    "elapsedMs", System.currentTimeMillis() - startTime
            ));
            return ExplorerTaskResult.builder()
                    .taskId(taskId)
                    .status(ExplorerTaskStatus.SUCCESS)
                    .summaryText(summary != null ? summary.getSummaryText() : null)
                    .objects(summary != null ? summary.getObjects() : List.of())
                    .rawResponse(summary != null ? summary.getRawResponse() : "")
                    .build();
        } catch (Exception e) {
            String errorMessage = StringUtils.defaultIfBlank(rootCauseMessage(e), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            log.warn("[Explorer] task failed, taskId={}, connectionId={}, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    taskId,
                    task.getConnectionId(),
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    e);
            agentLogService.recordDebug("CallingExplorerTool", "task_failed", AgentLogFields.of(
                    "taskId", taskId,
                    "connectionId", task.getConnectionId(),
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e),
                    "instructionPreview", preview(task.getInstruction()),
                    "contextPreview", preview(task.getContext())
            ));
            return ExplorerTaskResult.builder()
                    .taskId(taskId)
                    .status(ExplorerTaskStatus.ERROR)
                    .objects(List.of())
                    .rawResponse("")
                    .errorMessage(errorMessage)
                    .build();
        } finally {
            AgentExecutionContext.setParentToolCallId(previousParentToolCallId);
            AgentExecutionContext.setTaskId(previousTaskId);
            if (previousRequestContext != null) {
                RequestContext.set(previousRequestContext);
            } else {
                RequestContext.clear();
            }
            if (previousAgentRequestContext != null) {
                AgentRequestContext.set(previousAgentRequestContext);
            } else {
                AgentRequestContext.clear();
            }
        }
    }

    private String summarizeTasks(List<ExplorerTask> tasks) {
        List<String> summaries = new ArrayList<>();
        for (ExplorerTask task : tasks) {
            summaries.add("{connectionId=" + task.getConnectionId()
                    + ", instructionLength=" + StringUtils.length(task.getInstruction())
                    + ", contextLength=" + StringUtils.length(task.getContext())
                    + ", requestedTimeoutSeconds=" + task.getTimeoutSeconds()
                    + ", instructionPreview=" + preview(task.getInstruction())
                    + "}");
        }
        return summaries.toString();
    }

    private String summarizeTaskResults(List<ExplorerTaskResult> results) {
        List<String> summaries = new ArrayList<>();
        for (ExplorerTaskResult result : results) {
            summaries.add("{taskId=" + result.getTaskId()
                    + ", status=" + result.getStatus()
                    + ", objectCount=" + CollectionUtils.size(result.getObjects())
                    + ", errorMessage=" + preview(result.getErrorMessage())
                    + "}");
        }
        return summaries.toString();
    }

    private AgentToolResult buildExplorerAgentResult(List<ExplorerTask> tasks, ExplorerResultEnvelope envelope) {
        String resultJson = JsonUtil.object2json(envelope);
        String message = buildExplorerMessage(tasks, envelope.getTaskResults());
        return AgentToolResult.success(resultJson, message);
    }

    private String buildExplorerMessage(List<ExplorerTask> tasks, List<ExplorerTaskResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return ToolMessageSupport.sentence(
                    "Explorer finished without returning any task results.",
                    "Retry the exploration request before attempting SQL planning."
            );
        }
        List<String> failedTasks = new ArrayList<>();
        int successCount = 0;
        int discoveredObjectCount = 0;
        for (int index = 0; index < results.size(); index++) {
            ExplorerTaskResult result = results.get(index);
            if (result.getStatus() == ExplorerTaskStatus.ERROR) {
                ExplorerTask task = index < tasks.size() ? tasks.get(index) : null;
                failedTasks.add(describeTaskFailure(task, result));
                continue;
            }
            successCount++;
            discoveredObjectCount += CollectionUtils.size(result.getObjects());
        }
        if (failedTasks.isEmpty()) {
            if (discoveredObjectCount == 0) {
                return ToolMessageSupport.sentence(
                        "Explorer completed for " + successCount + " task(s) but did not return any matching objects.",
                        "Review the exploration scope and retry, or ask the user to clarify the target before planning SQL."
                );
            }
            return ToolMessageSupport.sentence(
                    "Explorer results are available for " + successCount + " task(s) with " + discoveredObjectCount + " discovered object(s).",
                    "Use the returned summaries and objects to continue planning.",
                    "If multiple targets remain plausible, ask the user to confirm the intended object before generating SQL."
            );
        }
        return ToolMessageSupport.sentence(
                "Explorer returned partial results. Failed tasks: " + String.join("; ", failedTasks) + ".",
                "Successful tasks returned " + discoveredObjectCount + " object(s).",
                ToolMessageSupport.continueOnlyWith("those successful results"),
                ToolMessageSupport.askUserWhether("switch connections, narrow the scope, or retry later"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }

    private String describeTaskFailure(ExplorerTask task, ExplorerTaskResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("connectionId=")
                .append(task != null ? task.getConnectionId() : "unknown");
        if (task != null && StringUtils.isNotBlank(task.getInstruction())) {
            builder.append(", instruction=\"")
                    .append(preview(task.getInstruction()))
                    .append("\"");
        }
        builder.append(", error=")
                .append(StringUtils.defaultIfBlank(result.getErrorMessage(), "unknown error"));
        return builder.toString();
    }

}
