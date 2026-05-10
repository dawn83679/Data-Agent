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

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingPlannerTool extends SubAgentToolSupport {

    private static final Logger runtimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.TOOL);

    private final SubAgentManager subAgentManager;
    private final SchemaSummaryResolver schemaSummaryResolver;

    @Tool({
            "价值：把已验证 schema 上下文转成 SQL 块和计划步骤。",
            "使用时机：已有可用 schema，且任务需要生成 SQL、优化 SQL 或产出多步骤执行计划。",
            "前置条件：schemaSummaryJson 必填，且必须已经包含本次 SQL 需求相关对象。",
            "结果：summaryText、planSteps、sqlBlocks 和 rawResponse。",
            "边界：instruction 必须描述一个具体 SQL 产出；宽泛对象发现应使用探索子代理。"
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("本次规划子代理调用的一个具体 SQL 目标。") String instruction,
            @P("已验证的 SchemaSummary JSON，必须包含相关对象。") String schemaSummaryJson,
            @P(value = "可选超时时间，单位秒；小于 180 会提升到 180。", required = false) Long timeoutSeconds,
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
                            "callingPlannerSubAgent 必须提供 schemaSummaryJson。",
                            "先调用 callingExplorerSubAgent 获取 schema 上下文。",
                            "必要 schema 摘要可用前不要生成 SQL 计划。"
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
                            "解析 schemaSummaryJson 失败：" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()) + "。",
                            "重试 callingPlannerSubAgent 前先修正 schema 摘要 payload。"
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
                            "schemaSummaryJson 不包含任何成功的探索任务结果。",
                            "先再次调用 callingExplorerSubAgent 或修复失败任务。",
                            "成功的探索输出可用前不要继续 SQL 规划。"
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
                    "SQL 规划已完成，但规划子代理没有返回可执行 SQL 块。",
                    "尝试执行前先检查计划摘要，并补充更多 schema 上下文。"
            );
        }
        return ToolMessageSupport.sentence(
                "SQL 计划已生成，包含 " + planStepCount + " 个计划步骤和 " + sqlBlockCount + " 个 SQL 块。",
                "执行前按用户目标复核计划。",
                "对于写入 SQL，如果 executeNonSelectSql 返回 REQUIRES_CONFIRMATION，等待用户确认后用完全相同的 SQL 重试 executeNonSelectSql。",
                "对于只读 SQL，只在目标对象和过滤条件已验证后执行。"
        );
    }

}
