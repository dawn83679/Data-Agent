package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.ExplorerSubAgentExecutorConfig;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.*;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
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

@AgentTool
@Slf4j
public class CallingExplorerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;
    private final Executor explorerExecutor;
    private final ConnectionAccessService connectionAccessService;

    public CallingExplorerTool(
            SubAgentManager subAgentManager,
            @Qualifier(ExplorerSubAgentExecutorConfig.EXPLORER_SUB_AGENT_EXECUTOR_BEAN) Executor explorerExecutor,
            ConnectionAccessService connectionAccessService) {
        this.subAgentManager = subAgentManager;
        this.explorerExecutor = explorerExecutor;
        this.connectionAccessService = connectionAccessService;
    }

    @Tool({
            "价值：运行一个或多个探索子代理，完成 schema 或对象发现。",
            "使用时机：缺少 schema 上下文，且搜索空间足够大，适合做聚焦或并行探索。",
            "前置条件：每个任务都需要 connectionId 和一个窄而可验证的 instruction；可能时写明目标名称。",
            "结果：taskResults，包含 taskId、summaryText、objects 和 rawResponse。",
            "边界：宽泛目标要先拆成聚焦任务，再调用此工具。"
    })
    public AgentToolResult callingExplorerSubAgent(
            @P("探索任务列表。每项需要 connectionId 和窄范围 instruction；context 和 timeoutSeconds 可选。") List<ExplorerTask> tasks,
            @P(value = "可选默认超时时间，单位秒；小于 180 会提升到 180。", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        if (CollectionUtils.isEmpty(tasks)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasks 必填。请提供 {connectionId, instruction, context?} 列表。"
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
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "探索子代理在 " + timeout + " 秒后超时",
                    "并发探索子代理在 " + timeout + " 秒后超时",
                    true,
                    e
            );
        } catch (Exception e) {
            log.error("[Tool] callingExplorerSubAgent concurrent failed, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    e);
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "并发探索子代理失败：" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()),
                    "并发探索子代理编排失败",
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
                        "tasks[" + index + "] 必填。"
                );
            }
            if (task.getConnectionId() == null || task.getConnectionId() <= 0) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].connectionId 必须是正数。"
                );
            }
            if (StringUtils.isBlank(task.getInstruction())) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].instruction 必填。"
                );
            }
            if (task.getTimeoutSeconds() != null && task.getTimeoutSeconds() <= 0) {
                throw AgentToolExecuteException.invalidInput(
                        ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                        "tasks[" + index + "].timeoutSeconds 必须是正数。"
                );
            }
            connectionAccessService.assertReadable(task.getConnectionId());
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
                    "探索子代理已结束，但没有返回任何任务结果。",
                    "尝试 SQL 规划前先重试探索请求。"
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
                        "探索子代理完成了 " + successCount + " 个任务，但没有返回匹配对象。",
                        "规划 SQL 前，先检查探索范围并重试，或询问用户明确目标。"
                );
            }
            return ToolMessageSupport.sentence(
                    "探索子代理已返回 " + successCount + " 个任务结果，共发现 " + discoveredObjectCount + " 个对象。",
                    "使用返回摘要和对象继续规划或直接检查。",
                    "如果某个返回目标已经足够明确，可以在该范围内继续 getObjectDetail 或 executeSelectSql。",
                    "如果仍有多个可能目标，生成 SQL 前先让用户确认目标对象或数据范围。"
            );
        }
        return ToolMessageSupport.sentence(
                "探索子代理返回了部分结果。失败任务：" + String.join("; ", failedTasks) + "。",
                "成功任务返回了 " + discoveredObjectCount + " 个对象。",
                ToolMessageSupport.continueOnlyWith("这些成功结果"),
                ToolMessageSupport.askUserWhether("切换连接、缩小范围或稍后重试"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }

    private String describeTaskFailure(ExplorerTask task, ExplorerTaskResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("connectionId=")
                .append(task != null ? task.getConnectionId() : "未知");
        if (task != null && StringUtils.isNotBlank(task.getInstruction())) {
            builder.append(", instruction=\"")
                    .append(preview(task.getInstruction()))
                    .append("\"");
        }
        builder.append(", 错误=")
                .append(StringUtils.defaultIfBlank(result.getErrorMessage(), "未知错误"));
        return builder.toString();
    }

}
