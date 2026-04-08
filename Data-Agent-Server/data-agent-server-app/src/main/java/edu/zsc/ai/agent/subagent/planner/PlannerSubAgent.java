package edu.zsc.ai.agent.subagent.planner;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.AbstractSubAgent;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.common.constant.AgentRuntimeLoggerNames;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Planner SubAgent implementation.
 * Uses TodoTool for task tracking and direct planner reasoning for SQL optimization.
 * Accepts SchemaSummary + user question, returns structured SqlPlan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerSubAgent extends AbstractSubAgent<PlannerRequest, SqlPlan> implements SubAgent<PlannerRequest, SqlPlan> {

    private static final Logger runtimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.SUB_AGENT);

    private final SubAgentFactory subAgentFactory;
    private final SubAgentProperties properties;
    private final AiModelCatalog aiModelCatalog;
    private final SubAgentStreamBridge streamBridge;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.PLANNER;
    }

    @Override
    public SqlPlan invoke(PlannerRequest request) {
        long startTime = System.currentTimeMillis();
        Long requestedTimeoutSeconds = request.getTimeoutSeconds();
        long timeoutSeconds = resolveTimeoutSeconds(requestedTimeoutSeconds, properties.getPlanner().getTimeoutSeconds());
        Long conversationId = resolveConversationId();
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        String taskId = AgentExecutionContext.getTaskId();
        String modelName = resolveModelName(log, aiModelCatalog);
        SchemaSummary schemaSummary = request.getSchemaSummary();

        // SSE progress emitter (replaces AgentListener-based observability)
        SubAgentObservabilityListener observer = new SubAgentObservabilityListener(
                AgentTypeEnum.PLANNER, conversationId, sseEmitterRegistry, null, taskId, parentToolCallId, null, timeoutSeconds);
        log.info("[Planner] invoke start, conversationId={}, taskId={}, parentToolCallId={}, modelName={}, requestedTimeoutSeconds={}, effectiveTimeoutSeconds={}, hasRequestContext={}, hasAgentContext={}, instructionLength={}, objectCount={}, rawResponsePresent={}, instructionPreview={}, objectPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                taskId,
                parentToolCallId,
                modelName,
                requestedTimeoutSeconds,
                timeoutSeconds,
                requestContextSnapshot != null,
                AgentRequestContext.hasContext(),
                StringUtils.length(request.getInstruction()),
                schemaSummary != null ? CollectionUtils.size(schemaSummary.getObjects()) : 0,
                schemaSummary != null && StringUtils.isNotBlank(schemaSummary.getRawResponse()),
                preview(request.getInstruction()),
                schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]");
        runtimeLog.info("planner_invoke_start conversationId={} taskId={} parentToolCallId={} modelName={} requestedTimeoutSeconds={} effectiveTimeoutSeconds={} instructionPreview={} objectPreview={} rawResponsePresent={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                taskId,
                parentToolCallId,
                modelName,
                requestedTimeoutSeconds,
                timeoutSeconds,
                preview(request.getInstruction()),
                schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]",
                schemaSummary != null && StringUtils.isNotBlank(schemaSummary.getRawResponse()));

        observer.emitStart();

        try {
            String message = buildMessage(request);
            log.info("[Planner] message built, taskId={}, messageLength={}, messagePreview={}",
                    taskId,
                    StringUtils.length(message),
                    preview(message));
            String systemPrompt = "(managed by SystemPromptManager)";
            PlannerAgentService agentService = subAgentFactory.buildPlannerAgent(modelName, conversationId);

            java.util.Map<String, Object> invocationContext = createInvocationContext(AgentTypeEnum.PLANNER);
            log.info("[Planner] schema summary serialized, taskId={}, objectCount={}, ddlObjectCount={}, summaryLength={}, rawResponseLength={}, objectPreview={}",
                    taskId,
                    schemaSummary != null ? CollectionUtils.size(schemaSummary.getObjects()) : 0,
                    countDdlObjects(schemaSummary),
                    schemaSummary != null ? StringUtils.length(schemaSummary.getSummaryText()) : 0,
                    schemaSummary != null ? StringUtils.length(schemaSummary.getRawResponse()) : 0,
                    schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]");
            log.info("[Planner] invocation context built, taskId={}, invocationKeys={}",
                    taskId,
                    invocationContext.keySet());
            runtimeLog.info("planner_model_request_ready taskId={} conversationId={} parentToolCallId={} modelName={} systemPromptLength={} systemPromptPreview={} messageLength={} messagePreview={} objectCount={} ddlObjectCount={} invocationKeys={}",
                    taskId,
                    conversationId,
                    parentToolCallId,
                    modelName,
                    StringUtils.length(systemPrompt),
                    preview(systemPrompt),
                    StringUtils.length(message),
                    preview(message),
                    schemaSummary != null ? CollectionUtils.size(schemaSummary.getObjects()) : 0,
                    countDdlObjects(schemaSummary),
                    invocationContext.keySet());
            InvocationParameters invocationParams = InvocationParameters.from(invocationContext);
            TokenStream tokenStream = agentService.plan(message, invocationParams);
            log.info("[Planner] token stream start, conversationId={}, taskId={}, parentToolCallId={}, timeoutSeconds={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    timeoutSeconds);
            runtimeLog.info("planner_token_stream_start taskId={} conversationId={} parentToolCallId={} timeoutSeconds={}",
                    taskId,
                    conversationId,
                    parentToolCallId,
                    timeoutSeconds);

            StringBuilder fullResponse = new StringBuilder();
            String parentId = parentToolCallId;
            Sinks.Many<ChatResponseBlock> sink = sseEmitterRegistry.get(conversationId).orElse(null);
            streamBridge.bridge(tokenStream, sink, parentId, null, fullResponse::append);

            CompletableFuture<String> future = new CompletableFuture<>();
            tokenStream.onCompleteResponse(response -> future.complete(fullResponse.toString()));
            tokenStream.onError(error -> future.completeExceptionally(error));
            tokenStream.start();

            String responseText;
            try {
                responseText = future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("[Planner] timeout, conversationId={}, taskId={}, timeoutSeconds={}, partialResponseLength={}, partialResponsePreview={}, elapsedMs={}",
                        conversationId,
                        taskId,
                        timeoutSeconds,
                        fullResponse.length(),
                        preview(fullResponse.toString()),
                        System.currentTimeMillis() - startTime,
                        e);
                runtimeLog.warn("planner_timeout taskId={} conversationId={} timeoutSeconds={} partialResponseLength={} partialResponsePreview={} elapsedMs={}",
                        taskId,
                        conversationId,
                        timeoutSeconds,
                        fullResponse.length(),
                        preview(fullResponse.toString()),
                        System.currentTimeMillis() - startTime);
                throw e;
            }
            log.info("[Planner] response received, taskId={}, responseLength={}, responsePreview={}",
                    taskId,
                    StringUtils.length(responseText),
                    preview(responseText));
            runtimeLog.info("planner_response_received taskId={} conversationId={} responseLength={} responsePreview={} response=\n{}",
                    taskId,
                    conversationId,
                    StringUtils.length(responseText),
                    preview(responseText),
                    responseText);
            SqlPlan plan = PlannerResponseParser.parse(responseText);

            observer.emitComplete(plan.getSummaryText(), JsonUtil.object2json(plan));
            log.info("[Planner] parse success, taskId={}, sqlBlockCount={}, stepCount={}, summaryLength={}, rawResponseLength={}, summaryPreview={}, elapsedMs={}",
                    taskId,
                    CollectionUtils.size(plan.getSqlBlocks()),
                    CollectionUtils.size(plan.getPlanSteps()),
                    StringUtils.length(plan.getSummaryText()),
                    StringUtils.length(plan.getRawResponse()),
                    preview(plan.getSummaryText()),
                    System.currentTimeMillis() - startTime);
            runtimeLog.info("planner_parse_success taskId={} conversationId={} sqlBlockCount={} planStepCount={} summaryLength={} rawResponseLength={} summaryPreview={} elapsedMs={} plan={}",
                    taskId,
                    conversationId,
                    CollectionUtils.size(plan.getSqlBlocks()),
                    CollectionUtils.size(plan.getPlanSteps()),
                    StringUtils.length(plan.getSummaryText()),
                    StringUtils.length(plan.getRawResponse()),
                    preview(plan.getSummaryText()),
                    System.currentTimeMillis() - startTime,
                    JsonUtil.object2json(plan));
            return plan;

        } catch (TimeoutException e) {
            String errorSummary = errorSummary(e, "Planner SubAgent timed out", timeoutSeconds);
            observer.emitError(errorSummary);
            runtimeLog.error("planner_invoke_failed conversationId={} taskId={} parentToolCallId={} elapsedMs={} rootCauseClass={} rootCauseMessage={} instructionPreview={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    preview(request.getInstruction()),
                    e);
            throw new RuntimeException("Planner SubAgent timed out: " + errorSummary, e);
        } catch (Exception e) {
            String errorSummary = errorSummary(e, "Planner SubAgent failed", timeoutSeconds);
            observer.emitError(errorSummary);
            log.error("[Planner] invoke failed, conversationId={}, taskId={}, parentToolCallId={}, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}, instructionPreview={}, objectPreview={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    preview(request.getInstruction()),
                    schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]",
                    e);
            runtimeLog.error("planner_invoke_failed conversationId={} taskId={} parentToolCallId={} elapsedMs={} rootCauseClass={} rootCauseMessage={} instructionPreview={} objectPreview={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    preview(request.getInstruction()),
                    schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]",
                    e);
            if (StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_calls")
                    && StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_call_id")) {
                runtimeLog.warn("planner_protocol_error_hint taskId={} conversationId={} hint={} nextCheck={}",
                        taskId,
                        conversationId,
                        "assistant tool_calls were emitted but matching tool messages were not present in the next model request",
                        "inspect planner-side message assembly or tool-result replay for this task");
            }
            throw new RuntimeException("Planner SubAgent failed: " + errorSummary, e);
        }
    }

    private String buildMessage(PlannerRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Instruction\n");
        sb.append(request.getInstruction()).append("\n\n");

        sb.append("## Schema Summary\n");
        sb.append(serializeSchemaSummary(request.getSchemaSummary())).append("\n");

        return sb.toString();
    }

    private String serializeSchemaSummary(SchemaSummary summary) {
        if (summary == null) {
            return "(no schema information available)\n";
        }

        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(summary.getObjects())) {
            for (ExploreObject object : summary.getObjects()) {
                sb.append("### ").append(object.getObjectName());
                if (StringUtils.isNotBlank(object.getObjectType())) {
                    sb.append(" [").append(object.getObjectType()).append("]");
                }
                if (object.getRelevanceScore() != null) {
                    sb.append(" {relevanceScore=").append(object.getRelevanceScore()).append("}");
                }
                sb.append("\n");

                if (StringUtils.isNotBlank(object.getCatalog())) {
                    sb.append("Catalog: ").append(object.getCatalog()).append("\n");
                }
                if (StringUtils.isNotBlank(object.getSchema())) {
                    sb.append("Schema: ").append(object.getSchema()).append("\n");
                }
                if (StringUtils.isNotBlank(object.getObjectDdl())) {
                    sb.append("DDL:\n");
                    sb.append(object.getObjectDdl()).append("\n");
                }

                sb.append("\n");
            }
        } else {
            sb.append("(no structured objects available)\n\n");
        }
        if (StringUtils.isNotBlank(summary.getRawResponse())) {
            sb.append("## Explorer Raw Response\n");
            sb.append(summary.getRawResponse()).append("\n");
        }
        return sb.toString();
    }

    private int countDdlObjects(SchemaSummary summary) {
        if (summary == null || CollectionUtils.isEmpty(summary.getObjects())) {
            return 0;
        }
        return (int) summary.getObjects().stream()
                .filter(object -> StringUtils.isNotBlank(object.getObjectDdl()))
                .count();
    }
}
