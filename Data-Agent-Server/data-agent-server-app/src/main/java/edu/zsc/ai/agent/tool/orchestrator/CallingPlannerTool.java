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
import edu.zsc.ai.common.constant.AgentRuntimeLoggerNames;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates SQL plan generation to Planner SubAgent.
 * Trace/span management is handled by SubAgentObservabilityListener (AgentListener).
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingPlannerTool extends SubAgentToolSupport {

    private static final Logger runtimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.TOOL);

    private final SubAgentManager subAgentManager;
    private final SchemaSummaryResolver schemaSummaryResolver;

    @Tool({
            "Value: turns verified schema context into a structured SQL plan with steps and SQL blocks you can review or execute.",
            "Use When: call after callingExplorerSubAgent or equivalent direct inspection has produced usable schema context and you need SQL generation or optimization.",
            "Task Scoping (CRITICAL): instruction must describe ONE concrete SQL outcome — a single query to write, a single statement to optimize, a single migration to draft. Do NOT pass umbrella goals like 'analyze the sales data', 'build a reporting layer', or 'write all the SQL we need'. If the user's request implies multiple SQL artefacts, call this tool once per artefact with a focused instruction each time, rather than asking one planner sub-agent to deliver several.",
            "Preconditions: schemaSummaryJson is required. It may be a SchemaSummary JSON or a callingExplorerSubAgent taskResults envelope JSON. The schemaSummary MUST already contain the objects relevant to the requested SQL — do not delegate broad 'figure out which tables apply' work to the planner; explore first.",
            "After Success: review the returned planSteps and sqlBlocks against the user goal. For write SQL, call executeNonSelectSql and respect its returned status. For read SQL, verify scope and then call executeSelectSql.",
            "After Failure: gather missing schema context or improve the planner instruction and retry. Do not execute guessed SQL.",
            "Result Consumption: use summaryText for explanation, planSteps for the execution outline, and sqlBlocks as candidate SQL to verify before running.",
            "Relation: usually after callingExplorerSubAgent or focused direct inspection and before executeSelectSql or executeNonSelectSql. When the current scope is already explicit and the read task is simple, direct execution may be better than planner delegation. Planner timeout defaults to 180 seconds, and lower values are raised to 180."
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("Single, concrete SQL goal for this invocation. Examples: 'aggregate daily revenue from orders for the last 30 days, grouped by region', 'rewrite the slow query <SQL> using the orders_idx_created_at index', 'draft a migration that adds non-null status column to public.invoice with backfill default'. Do NOT pass multi-task umbrella instructions ('analyze sales', 'build reports') — split into multiple invocations of this tool. Include optimization context (current SQL, observed plan, latency) only when it pertains to this single goal.") String instruction,
            @P("SchemaSummary JSON from a previous callingExplorerSubAgent result") String schemaSummaryJson,
            @P(value = "Optional timeout in seconds for this planner sub-agent invocation. Default is 180 seconds. Values below 180 are automatically raised to 180.", required = false) Long timeoutSeconds,
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
        runtimeLog.info("calling_planner_start conversationId={} parentToolCallId={} taskId={} requestedTimeoutSeconds={} effectiveTimeoutSeconds={} minimumTimeoutSeconds={} instructionPreview={} schemaSummaryJsonLength={} schemaSummaryPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : null,
                AgentExecutionContext.getParentToolCallId(),
                taskId,
                timeoutSeconds,
                effectiveTimeoutSeconds,
                SubAgentTimeoutPolicy.MIN_TIMEOUT_SECONDS,
                preview(instruction),
                StringUtils.length(schemaSummaryJson),
                preview(schemaSummaryJson));
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
        runtimeLog.info("calling_planner_schema_normalized taskId={} objectCount={} summaryLength={} rawResponseLength={} objectPreview={}",
                taskId,
                CollectionUtils.size(schemaSummary.getObjects()),
                StringUtils.length(schemaSummary.getSummaryText()),
                StringUtils.length(schemaSummary.getRawResponse()),
                summarizeObjects(schemaSummary.getObjects()));
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
        runtimeLog.info("calling_planner_request_built taskId={} instructionLength={} instructionPreview={} objectCount={} requestedTimeoutSeconds={} effectiveTimeoutSeconds={} summaryPreview={} request={}",
                taskId,
                StringUtils.length(instruction),
                preview(instruction),
                CollectionUtils.size(schemaSummary.getObjects()),
                timeoutSeconds,
                effectiveTimeoutSeconds,
                preview(schemaSummary.getSummaryText()),
                JsonUtil.object2json(request));

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
            runtimeLog.error("calling_planner_invoke_failed taskId={} elapsedMs={} rootCauseClass={} rootCauseMessage={} instructionPreview={} objectPreview={}",
                    taskId,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    preview(instruction),
                    summarizeObjects(schemaSummary.getObjects()),
                    e);
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
        runtimeLog.info("calling_planner_done taskId={} elapsedMs={} sqlBlockCount={} planStepCount={} summaryLength={} rawResponseLength={} summaryPreview={} plan={}",
                taskId,
                System.currentTimeMillis() - startTime,
                CollectionUtils.size(plan.getSqlBlocks()),
                CollectionUtils.size(plan.getPlanSteps()),
                StringUtils.length(plan.getSummaryText()),
                StringUtils.length(plan.getRawResponse()),
                preview(plan.getSummaryText()),
                JsonUtil.object2json(plan));
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
