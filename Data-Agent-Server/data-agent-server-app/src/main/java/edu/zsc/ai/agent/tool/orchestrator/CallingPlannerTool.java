package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.AgentExecutionContext;
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

/**
 * Delegates SQL plan generation to Planner SubAgent.
 * Trace/span management is handled by SubAgentObservabilityListener (AgentListener).
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingPlannerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;

    @Tool({
            "Delegates SQL plan generation to Planner SubAgent.",
            "Use when: you already have schema context from callingExplorerSubAgent and need to produce SQL.",
            "Accepts either a SchemaSummary JSON or callingExplorerSubAgent taskResults envelope JSON.",
            "Returns: SqlPlan JSON with summaryText, planSteps, sqlBlocks, and rawResponse.",
            "Include optimization context (existing SQL, DDLs, indexes) in instruction if needed."
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("Task instruction - describe what SQL to generate, include optimization context if needed") String instruction,
            @P("SchemaSummary JSON from a previous callingExplorerSubAgent result") String schemaSummaryJson,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String taskId = buildTaskId(requestContextSnapshot);
        log.info("[Tool] callingPlannerSubAgent start, conversationId={}, parentToolCallId={}, instructionLength={}, schemaSummaryJsonLength={}, instructionPreview={}, schemaSummaryPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                AgentExecutionContext.getParentToolCallId(),
                StringUtils.length(instruction),
                StringUtils.length(schemaSummaryJson),
                preview(instruction),
                preview(schemaSummaryJson));
        SubAgentDebugWriter.append("CallingPlannerTool", "tool_start", SubAgentDebugWriter.fields(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", AgentExecutionContext.getParentToolCallId(),
                "taskId", taskId,
                "instructionPreview", preview(instruction),
                "schemaSummaryJsonLength", StringUtils.length(schemaSummaryJson),
                "schemaSummaryPreview", preview(schemaSummaryJson)
        ));
        return invokePlanner(instruction, schemaSummaryJson, taskId);
    }

    private AgentToolResult invokePlanner(String instruction, String schemaSummaryJson, String taskId) {
        long startTime = System.currentTimeMillis();
        if (StringUtils.isBlank(schemaSummaryJson)) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.CALLING_PLANNER_SUB_AGENT,
                    "schemaSummaryJson is required for callingPlannerSubAgent. Call callingExplorerSubAgent first to get schema."
            );
        }

        SchemaSummary schemaSummary;
        try {
            schemaSummary = parseSchemaSummary(schemaSummaryJson);
        } catch (Exception e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_PLANNER_SUB_AGENT,
                    "Failed to parse schemaSummaryJson: " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName())
            );
        }
        log.info("[Planner] schemaSummary normalized, objectCount={}, summaryLength={}, rawResponseLength={}, objectPreview={}",
                CollectionUtils.size(schemaSummary.getObjects()),
                StringUtils.length(schemaSummary.getSummaryText()),
                StringUtils.length(schemaSummary.getRawResponse()),
                summarizeObjects(schemaSummary.getObjects()));
        SubAgentDebugWriter.append("CallingPlannerTool", "schema_summary_normalized", SubAgentDebugWriter.fields(
                "taskId", taskId,
                "objectCount", CollectionUtils.size(schemaSummary.getObjects()),
                "summaryLength", StringUtils.length(schemaSummary.getSummaryText()),
                "rawResponseLength", StringUtils.length(schemaSummary.getRawResponse()),
                "objectPreview", summarizeObjects(schemaSummary.getObjects())
        ));
        if (CollectionUtils.isEmpty(schemaSummary.getObjects())
                && StringUtils.isBlank(schemaSummary.getRawResponse())
                && StringUtils.isBlank(schemaSummary.getSummaryText())) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.CALLING_PLANNER_SUB_AGENT,
                    "schemaSummaryJson does not contain any successful explorer task results. Call callingExplorerSubAgent again or fix the failed tasks first."
            );
        }

        PlannerRequest request = PlannerRequest.builder()
                .instruction(instruction)
                .schemaSummary(schemaSummary)
                .build();

        log.info("[Planner] request built, instructionLength={}, objectCount={}, summaryPreview={}",
                StringUtils.length(instruction),
                CollectionUtils.size(schemaSummary.getObjects()),
                preview(schemaSummary.getSummaryText()));
        SubAgentDebugWriter.append("CallingPlannerTool", "request_built", SubAgentDebugWriter.fields(
                "taskId", taskId,
                "instructionLength", StringUtils.length(instruction),
                "instructionPreview", preview(instruction),
                "objectCount", CollectionUtils.size(schemaSummary.getObjects()),
                "summaryPreview", preview(schemaSummary.getSummaryText())
        ));

        String previousParentToolCallId = AgentExecutionContext.getParentToolCallId();
        String previousTaskId = AgentExecutionContext.getTaskId();
        SqlPlan plan;
        try {
            AgentExecutionContext.setParentToolCallId(previousParentToolCallId);
            AgentExecutionContext.setTaskId(taskId);
            plan = subAgentManager.getPlannerSubAgent().invoke(request);
        } catch (Exception e) {
            log.error("[Planner] subagent invoke failed, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}, instructionPreview={}, objectPreview={}",
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    preview(instruction),
                    summarizeObjects(schemaSummary.getObjects()),
                    e);
            SubAgentDebugWriter.append("CallingPlannerTool", "subagent_invoke_failed", SubAgentDebugWriter.fields(
                    "taskId", taskId,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e),
                    "instructionPreview", preview(instruction),
                    "objectPreview", summarizeObjects(schemaSummary.getObjects())
            ));
            throw e;
        } finally {
            AgentExecutionContext.setParentToolCallId(previousParentToolCallId);
            AgentExecutionContext.setTaskId(previousTaskId);
        }

        log.info("[Tool done] callingPlannerSubAgent, elapsedMs={}, sqlBlockCount={}, planStepCount={}, summaryLength={}, rawResponseLength={}, summaryPreview={}",
                System.currentTimeMillis() - startTime,
                CollectionUtils.size(plan.getSqlBlocks()),
                CollectionUtils.size(plan.getPlanSteps()),
                StringUtils.length(plan.getSummaryText()),
                StringUtils.length(plan.getRawResponse()),
                preview(plan.getSummaryText()));
        SubAgentDebugWriter.append("CallingPlannerTool", "tool_done", SubAgentDebugWriter.fields(
                "taskId", taskId,
                "elapsedMs", System.currentTimeMillis() - startTime,
                "sqlBlockCount", CollectionUtils.size(plan.getSqlBlocks()),
                "planStepCount", CollectionUtils.size(plan.getPlanSteps()),
                "summaryLength", StringUtils.length(plan.getSummaryText()),
                "rawResponseLength", StringUtils.length(plan.getRawResponse()),
                "summaryPreview", preview(plan.getSummaryText())
        ));
        return AgentToolResult.success(JsonUtil.object2json(plan));
    }

    private SchemaSummary parseSchemaSummary(String schemaSummaryJson) {
        try {
            ExplorerResultEnvelope envelope = JsonUtil.json2Object(schemaSummaryJson, ExplorerResultEnvelope.class);
            if (envelope != null && CollectionUtils.isNotEmpty(envelope.getTaskResults())) {
                List<ExploreObject> mergedObjects = new ArrayList<>();
                StringBuilder summaryText = new StringBuilder();
                StringBuilder rawResponse = new StringBuilder();
                for (ExplorerTaskResult taskResult : envelope.getTaskResults()) {
                    if (taskResult.getStatus() == ExplorerTaskStatus.ERROR) {
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(taskResult.getObjects())) {
                        mergedObjects.addAll(taskResult.getObjects());
                    }
                    if (StringUtils.isNotBlank(taskResult.getSummaryText())) {
                        if (summaryText.length() > 0) summaryText.append("\n");
                        summaryText.append(taskResult.getSummaryText());
                    }
                    if (StringUtils.isNotBlank(taskResult.getRawResponse())) {
                        if (rawResponse.length() > 0) rawResponse.append("\n\n");
                        rawResponse.append(taskResult.getRawResponse());
                    }
                }
                return SchemaSummary.builder()
                        .summaryText(summaryText.toString())
                        .rawResponse(rawResponse.toString())
                        .objects(mergedObjects)
                        .build();
            }
        } catch (Exception ignored) {
            // Fall through to plain SchemaSummary parsing.
        }
        return JsonUtil.json2Object(schemaSummaryJson, SchemaSummary.class);
    }

    private String summarizeObjects(List<ExploreObject> objects) {
        if (CollectionUtils.isEmpty(objects)) {
            return "[]";
        }
        List<String> names = new ArrayList<>();
        for (ExploreObject object : objects.stream().limit(3).toList()) {
            names.add(qualifiedName(object));
        }
        return names.toString();
    }

    private String qualifiedName(ExploreObject object) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(object.getCatalog())) {
            parts.add(object.getCatalog());
        }
        if (StringUtils.isNotBlank(object.getSchema())) {
            parts.add(object.getSchema());
        }
        if (StringUtils.isNotBlank(object.getObjectName())) {
            parts.add(object.getObjectName());
        }
        return String.join(".", parts);
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

    private String buildTaskId(RequestContextInfo requestContextSnapshot) {
        return "plan-" + (requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : "0")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
