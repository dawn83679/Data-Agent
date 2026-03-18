package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTask;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.util.SubAgentDebugWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

/**
 * Delegates schema exploration to Explorer SubAgent(s).
 * Accepts a list of ExplorerTask — each task spawns one Explorer SubAgent.
 * Multiple tasks run concurrently; results are returned task-by-task to MainAgent.
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingExplorerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;

    @Tool({
            "Delegates schema exploration to Explorer SubAgent(s).",
            "Use when: you need to understand database structure before generating SQL.",
            "Accepts a JSON array of tasks. Each task has: connectionId (required), instruction (required), context (optional).",
            "Each task spawns one Explorer SubAgent. Multiple tasks run concurrently.",
            "Returns: JSON object with taskResults[]. Each taskResult includes taskId, summaryText, objects, rawResponse.",
            "connectionId from getEnvironmentOverview.",
            "Flow: callingExplorerSubAgent -> confirm with user -> callingPlannerSubAgent -> confirm -> execute."
    })
    public AgentToolResult callingExplorerSubAgent(
            @P("JSON array of explorer tasks. Each element: {connectionId: number, instruction: string, context?: string}") String tasksJson,
            @P(value = "Optional timeout in seconds for each SubAgent. Defaults to configured timeout if not provided.", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        List<ExplorerTask> tasks = parseTasks(tasksJson);
        if (CollectionUtils.isEmpty(tasks)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasks is required. Provide a JSON array of {connectionId, instruction, context?}."
            );
        }

        log.info("[Tool] callingExplorerSubAgent start, conversationId={}, parentToolCallId={}, taskCount={}, timeoutSeconds={}, tasks={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                AgentExecutionContext.getParentToolCallId(),
                tasks.size(),
                timeoutSeconds,
                summarizeTasks(tasks));
        SubAgentDebugWriter.append("CallingExplorerTool", "tool_start", SubAgentDebugWriter.fields(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", AgentExecutionContext.getParentToolCallId(),
                "taskCount", tasks.size(),
                "timeoutSeconds", timeoutSeconds,
                "tasks", summarizeTasks(tasks)
        ));

        if (tasks.size() == 1) {
            return invokeSingle(tasks.get(0), timeoutSeconds);
        }

        return invokeConcurrent(tasks, timeoutSeconds);
    }

    private List<ExplorerTask> parseTasks(String tasksJson) {
        if (StringUtils.isBlank(tasksJson)) {
            return null;
        }
        try {
            return JsonUtil.json2List(tasksJson, ExplorerTask.class);
        } catch (Exception e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasksJson must be a valid JSON array of {connectionId, instruction, context?}."
            );
        }
    }

    private AgentToolResult invokeSingle(ExplorerTask task, Long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        ExplorerTaskResult result = executeTask(
                task,
                timeoutSeconds,
                requestContextSnapshot,
                agentRequestContextSnapshot,
                AgentExecutionContext.getParentToolCallId(),
                buildTaskId(requestContextSnapshot)
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
        SubAgentDebugWriter.append("CallingExplorerTool", "tool_done_single", SubAgentDebugWriter.fields(
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
        long timeout = timeoutSeconds != null && timeoutSeconds > 0
                ? timeoutSeconds
                : subAgentManager.getProperties().getExplorer().getTimeoutSeconds();

        log.info("[Tool] callingExplorerSubAgent concurrent dispatch, parentToolCallId={}, timeoutSeconds={}, taskCount={}",
                parentToolCallId,
                timeout,
                tasks.size());
        SubAgentDebugWriter.append("CallingExplorerTool", "concurrent_dispatch", SubAgentDebugWriter.fields(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", parentToolCallId,
                "timeoutSeconds", timeout,
                "taskCount", tasks.size()
        ));

        List<CompletableFuture<ExplorerTaskResult>> futures = IntStream.range(0, tasks.size())
                .mapToObj(i -> {
                    ExplorerTask task = tasks.get(i);
                    String taskId = buildTaskId(requestContextSnapshot);
                    return CompletableFuture.supplyAsync(() -> executeTask(
                            task, timeoutSeconds, requestContextSnapshot, agentRequestContextSnapshot, parentToolCallId, taskId));
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
            SubAgentDebugWriter.append("CallingExplorerTool", "tool_done_concurrent", SubAgentDebugWriter.fields(
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
            SubAgentDebugWriter.append("CallingExplorerTool", "concurrent_timeout", SubAgentDebugWriter.fields(
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
            SubAgentDebugWriter.append("CallingExplorerTool", "concurrent_failed", SubAgentDebugWriter.fields(
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

    private ExplorerTaskResult executeTask(ExplorerTask task, Long timeoutSeconds,
                                           RequestContextInfo requestContextSnapshot,
                                           AgentRequestContextInfo agentRequestContextSnapshot,
                                           String parentToolCallId, String taskId) {
        long startTime = System.currentTimeMillis();
        log.info("[Explorer] task start, taskId={}, parentToolCallId={}, conversationId={}, connectionId={}, timeoutSeconds={}, instructionLength={}, contextLength={}, instructionPreview={}, contextPreview={}",
                taskId,
                parentToolCallId,
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                task.getConnectionId(),
                timeoutSeconds,
                StringUtils.length(task.getInstruction()),
                StringUtils.length(task.getContext()),
                preview(task.getInstruction()),
                preview(task.getContext()));
        SubAgentDebugWriter.append("CallingExplorerTool", "task_start", SubAgentDebugWriter.fields(
                "taskId", taskId,
                "parentToolCallId", parentToolCallId,
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "connectionId", task.getConnectionId(),
                "timeoutSeconds", timeoutSeconds,
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
                    task.getContext());
            SchemaSummary summary = subAgentManager.getExplorerSubAgent().invoke(request);
            log.info("[Explorer] task success, taskId={}, connectionId={}, objectCount={}, summaryLength={}, rawResponseLength={}, summaryPreview={}, elapsedMs={}",
                    taskId,
                    task.getConnectionId(),
                    summary != null ? CollectionUtils.size(summary.getObjects()) : 0,
                    summary != null ? StringUtils.length(summary.getSummaryText()) : 0,
                    summary != null ? StringUtils.length(summary.getRawResponse()) : 0,
                    summary != null ? preview(summary.getSummaryText()) : null,
                    System.currentTimeMillis() - startTime);
            SubAgentDebugWriter.append("CallingExplorerTool", "task_success", SubAgentDebugWriter.fields(
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
            String errorMessage = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            log.warn("[Explorer] task failed, taskId={}, connectionId={}, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    taskId,
                    task.getConnectionId(),
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    e);
            SubAgentDebugWriter.append("CallingExplorerTool", "task_failed", SubAgentDebugWriter.fields(
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

    private String buildTaskId(RequestContextInfo requestContextSnapshot) {
        return "explore-" + (requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : "0")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String summarizeTasks(List<ExplorerTask> tasks) {
        List<String> summaries = new ArrayList<>();
        for (ExplorerTask task : tasks) {
            summaries.add("{connectionId=" + task.getConnectionId()
                    + ", instructionLength=" + StringUtils.length(task.getInstruction())
                    + ", contextLength=" + StringUtils.length(task.getContext())
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
        if (StringUtils.isBlank(message)) {
            return AgentToolResult.success(resultJson);
        }
        return AgentToolResult.builder()
                .success(true)
                .message(message)
                .result(resultJson)
                .build();
    }

    private String buildExplorerMessage(List<ExplorerTask> tasks, List<ExplorerTaskResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        List<String> failedTasks = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            ExplorerTaskResult result = results.get(index);
            if (result.getStatus() != ExplorerTaskStatus.ERROR) {
                continue;
            }
            ExplorerTask task = index < tasks.size() ? tasks.get(index) : null;
            failedTasks.add("connectionId="
                    + (task != null ? task.getConnectionId() : "unknown")
                    + " (" + StringUtils.defaultIfBlank(result.getErrorMessage(), "unknown error") + ")");
        }
        if (failedTasks.isEmpty()) {
            return null;
        }
        return "Explorer failed for: " + String.join("; ", failedTasks) + ". Ask the user whether to switch connections, narrow the scope, or retry later. Do not continue object discovery until the user replies.";
    }

    private String preview(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return StringUtils.abbreviate(StringUtils.normalizeSpace(value), 160);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = rootCause(throwable);
        return StringUtils.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName());
    }
}
