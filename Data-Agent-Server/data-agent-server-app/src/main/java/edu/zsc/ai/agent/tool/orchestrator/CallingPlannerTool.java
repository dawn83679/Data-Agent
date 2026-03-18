package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
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

/**
 * Delegates SQL plan generation to Planner SubAgent.
 * Trace/span management is handled by SubAgentObservabilityListener (AgentListener).
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingPlannerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;
    private final SchemaSummaryResolver schemaSummaryResolver;

    @Tool({
            "Delegates SQL plan generation to Planner SubAgent.",
            "Use when: you already have schema context from callingExplorerSubAgent and need to produce SQL.",
            "Accepts either a SchemaSummary JSON or callingExplorerSubAgent taskResults envelope JSON.",
            "Returns: SqlPlan JSON with summaryText, planSteps, sqlBlocks, and rawResponse.",
            "Include optimization context (existing SQL, DDLs, indexes) in instruction if needed.",
            "Optional timeoutSeconds overrides the planner sub-agent timeout for this invocation."
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("Task instruction - describe what SQL to generate, include optimization context if needed") String instruction,
            @P("SchemaSummary JSON from a previous callingExplorerSubAgent result") String schemaSummaryJson,
            @P(value = "Optional timeout in seconds for this planner sub-agent invocation.", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String taskId = buildTaskId("plan", requestContextSnapshot);
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
        return invokePlanner(instruction, schemaSummaryJson, timeoutSeconds, taskId);
    }

    private AgentToolResult invokePlanner(String instruction, String schemaSummaryJson, Long timeoutSeconds, String taskId) {
        long startTime = System.currentTimeMillis();
        if (StringUtils.isBlank(schemaSummaryJson)) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.CALLING_PLANNER_SUB_AGENT,
                    "schemaSummaryJson is required for callingPlannerSubAgent. Call callingExplorerSubAgent first to get schema."
            );
        }

        SchemaSummary schemaSummary;
        try {
            schemaSummary = schemaSummaryResolver.resolve(schemaSummaryJson);
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
                .timeoutSeconds(resolveTimeoutSeconds(timeoutSeconds, subAgentManager.getProperties().getPlanner().getTimeoutSeconds()))
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

}
