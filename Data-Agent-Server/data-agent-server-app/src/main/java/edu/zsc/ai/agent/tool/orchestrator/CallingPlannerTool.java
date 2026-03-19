package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentTimeoutPolicy;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.util.JsonUtil;
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
    private final AgentLogService agentLogService;

    @Tool({
            "Value: turns verified schema context into a structured SQL plan with steps and SQL blocks you can review or execute.",
            "Use When: call after callingExplorerSubAgent or equivalent discovery has produced usable schema context and you need SQL generation or optimization.",
            "Preconditions: schemaSummaryJson is required. It may be a SchemaSummary JSON or a callingExplorerSubAgent taskResults envelope JSON. Include optimization context in instruction when relevant.",
            "After Success: review the returned planSteps and sqlBlocks against the user goal. For write SQL, call executeNonSelectSql and respect its returned status. For read SQL, verify scope and then call executeSelectSql.",
            "After Failure: gather missing schema context or improve the planner instruction and retry. Do not execute guessed SQL.",
            "Result Consumption: use summaryText for explanation, planSteps for the execution outline, and sqlBlocks as candidate SQL to verify before running.",
            "Relation: usually after callingExplorerSubAgent and before executeSelectSql or executeNonSelectSql. Planner timeout defaults to 180 seconds, and lower values are raised to 120."
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("Task instruction - describe what SQL to generate, include optimization context if needed") String instruction,
            @P("SchemaSummary JSON from a previous callingExplorerSubAgent result") String schemaSummaryJson,
            @P(value = "Optional timeout in seconds for this planner sub-agent invocation. Default is 180 seconds. Values below 120 are automatically raised to 120.", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String taskId = buildTaskId("plan", requestContextSnapshot);
        long effectiveTimeoutSeconds = resolveTimeoutSeconds(timeoutSeconds, subAgentManager.getProperties().getPlanner().getTimeoutSeconds());
        log.info("[Tool] callingPlannerSubAgent start, conversationId={}, parentToolCallId={}, requestedTimeoutSeconds={}, effectiveTimeoutSeconds={}, instructionLength={}, schemaSummaryJsonLength={}, instructionPreview={}, schemaSummaryPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                AgentExecutionContext.getParentToolCallId(),
                timeoutSeconds,
                effectiveTimeoutSeconds,
                StringUtils.length(instruction),
                StringUtils.length(schemaSummaryJson),
                preview(instruction),
                preview(schemaSummaryJson));
        agentLogService.recordDebug("CallingPlannerTool", "tool_start", AgentLogFields.of(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                "parentToolCallId", AgentExecutionContext.getParentToolCallId(),
                "taskId", taskId,
                "requestedTimeoutSeconds", timeoutSeconds,
                "effectiveTimeoutSeconds", effectiveTimeoutSeconds,
                "minimumTimeoutSeconds", SubAgentTimeoutPolicy.MIN_TIMEOUT_SECONDS,
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
                    ToolMessageSupport.sentence(
                            "schemaSummaryJson is required for callingPlannerSubAgent.",
                            "Call callingExplorerSubAgent first to gather schema context.",
                            "Do not generate SQL plans until the required schema summary is available."
                    )
            );
        }

        SchemaSummary schemaSummary;
        try {
            schemaSummary = schemaSummaryResolver.resolve(schemaSummaryJson);
        } catch (Exception e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_PLANNER_SUB_AGENT,
                    ToolMessageSupport.sentence(
                            "Failed to parse schemaSummaryJson: " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()) + ".",
                            "Fix the schema summary payload before retrying callingPlannerSubAgent."
                    )
            );
        }
        log.info("[Planner] schemaSummary normalized, objectCount={}, summaryLength={}, rawResponseLength={}, objectPreview={}",
                CollectionUtils.size(schemaSummary.getObjects()),
                StringUtils.length(schemaSummary.getSummaryText()),
                StringUtils.length(schemaSummary.getRawResponse()),
                summarizeObjects(schemaSummary.getObjects()));
        agentLogService.recordDebug("CallingPlannerTool", "schema_summary_normalized", AgentLogFields.of(
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
                    ToolMessageSupport.sentence(
                            "schemaSummaryJson does not contain any successful explorer task results.",
                            "Call callingExplorerSubAgent again or fix the failed tasks first.",
                            "Do not continue to SQL planning until successful explorer output is available."
                    )
            );
        }

        long effectiveTimeoutSeconds = resolveTimeoutSeconds(timeoutSeconds, subAgentManager.getProperties().getPlanner().getTimeoutSeconds());
        PlannerRequest request = PlannerRequest.builder()
                .instruction(instruction)
                .schemaSummary(schemaSummary)
                .timeoutSeconds(effectiveTimeoutSeconds)
                .build();

        log.info("[Planner] request built, instructionLength={}, objectCount={}, requestedTimeoutSeconds={}, effectiveTimeoutSeconds={}, summaryPreview={}",
                StringUtils.length(instruction),
                CollectionUtils.size(schemaSummary.getObjects()),
                timeoutSeconds,
                effectiveTimeoutSeconds,
                preview(schemaSummary.getSummaryText()));
        agentLogService.recordDebug("CallingPlannerTool", "request_built", AgentLogFields.of(
                "taskId", taskId,
                "instructionLength", StringUtils.length(instruction),
                "instructionPreview", preview(instruction),
                "objectCount", CollectionUtils.size(schemaSummary.getObjects()),
                "requestedTimeoutSeconds", timeoutSeconds,
                "effectiveTimeoutSeconds", effectiveTimeoutSeconds,
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
            agentLogService.recordDebug("CallingPlannerTool", "subagent_invoke_failed", AgentLogFields.of(
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
        agentLogService.recordDebug("CallingPlannerTool", "tool_done", AgentLogFields.of(
                "taskId", taskId,
                "elapsedMs", System.currentTimeMillis() - startTime,
                "sqlBlockCount", CollectionUtils.size(plan.getSqlBlocks()),
                "planStepCount", CollectionUtils.size(plan.getPlanSteps()),
                "summaryLength", StringUtils.length(plan.getSummaryText()),
                "rawResponseLength", StringUtils.length(plan.getRawResponse()),
                "summaryPreview", preview(plan.getSummaryText())
        ));
        return AgentToolResult.success(JsonUtil.object2json(plan), buildPlannerSuccessMessage(plan));
    }

    private String buildPlannerSuccessMessage(SqlPlan plan) {
        int sqlBlockCount = CollectionUtils.size(plan.getSqlBlocks());
        int planStepCount = CollectionUtils.size(plan.getPlanSteps());
        if (sqlBlockCount == 0) {
            return ToolMessageSupport.sentence(
                    "SQL planning completed, but the planner did not return executable SQL blocks.",
                    "Review the plan summary and gather more schema context before attempting execution."
            );
        }
        return ToolMessageSupport.sentence(
                "SQL plan is available with " + planStepCount + " plan step(s) and " + sqlBlockCount + " SQL block(s).",
                "Review the plan against the user's goal before executing it.",
                "For write SQL, if executeNonSelectSql returns REQUIRES_CONFIRMATION, wait for the user's confirmation and then retry executeNonSelectSql with the exact same SQL.",
                "For read SQL, execute only after the target objects and filters are verified."
        );
    }

}
