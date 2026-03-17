package edu.zsc.ai.agent.subagent.planner;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.util.SubAgentDebugWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Planner SubAgent implementation.
 * Uses TodoTool for task tracking,
 * and ActivateSkillTool for optional SQL optimization.
 * Accepts SchemaSummary + user question, returns structured SqlPlan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerSubAgent implements SubAgent<PlannerRequest, SqlPlan> {

    private final SubAgentFactory subAgentFactory;
    private final SubAgentProperties properties;
    private final SubAgentStreamBridge streamBridge;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.PLANNER;
    }

    @Override
    public SqlPlan invoke(PlannerRequest request) {
        long startTime = System.currentTimeMillis();
        Long conversationId = resolveConversationId();
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        String taskId = AgentExecutionContext.getTaskId();
        String modelName = resolveModelName();
        SchemaSummary schemaSummary = request.getSchemaSummary();

        // SSE progress emitter (replaces AgentListener-based observability)
        SubAgentObservabilityListener observer = new SubAgentObservabilityListener(
                AgentTypeEnum.PLANNER, conversationId, sseEmitterRegistry, null, taskId, parentToolCallId);
        log.info("[Planner] invoke start, conversationId={}, taskId={}, parentToolCallId={}, modelName={}, hasRequestContext={}, hasAgentContext={}, instructionLength={}, objectCount={}, rawResponsePresent={}, instructionPreview={}, objectPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                taskId,
                parentToolCallId,
                modelName,
                requestContextSnapshot != null,
                AgentRequestContext.hasContext(),
                StringUtils.length(request.getInstruction()),
                schemaSummary != null ? CollectionUtils.size(schemaSummary.getObjects()) : 0,
                schemaSummary != null && StringUtils.isNotBlank(schemaSummary.getRawResponse()),
                preview(request.getInstruction()),
                schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]");
        SubAgentDebugWriter.append("PlannerSubAgent", "invoke_start", SubAgentDebugWriter.fields(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                "taskId", taskId,
                "parentToolCallId", parentToolCallId,
                "modelName", modelName,
                "instructionPreview", preview(request.getInstruction()),
                "objectPreview", schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]",
                "rawResponsePresent", schemaSummary != null && StringUtils.isNotBlank(schemaSummary.getRawResponse())
        ));

        observer.emitStart();

        try {
            String message = buildMessage(request);
            log.info("[Planner] message built, taskId={}, messageLength={}, messagePreview={}",
                    taskId,
                    StringUtils.length(message),
                    preview(message));
            String systemPrompt = PromptConfig.getPrompt(PromptEnum.PLANNER);

            // Build fresh agent per invocation with AgentBuilder (via SubAgentFactory)
            PlannerAgentService agentService = subAgentFactory.buildPlannerAgent(
                    modelName, systemPrompt);

            HashMap<String, Object> invocationContext = new HashMap<>(RequestContext.toMap());
            invocationContext.putAll(AgentRequestContext.toMap());
            invocationContext.put(InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.PLANNER.getCode());
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
            SubAgentDebugWriter.append("PlannerSubAgent", "model_request_ready", SubAgentDebugWriter.fields(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "parentToolCallId", parentToolCallId,
                    "modelName", modelName,
                    "systemPromptLength", StringUtils.length(systemPrompt),
                    "systemPromptPreview", preview(systemPrompt),
                    "messageLength", StringUtils.length(message),
                    "messagePreview", preview(message),
                    "objectCount", schemaSummary != null ? CollectionUtils.size(schemaSummary.getObjects()) : 0,
                    "ddlObjectCount", countDdlObjects(schemaSummary),
                    "invocationKeys", invocationContext.keySet(),
                    "messageProtocolHint", "agentService API does not expose raw tool-call messages before dispatch"
            ));
            InvocationParameters invocationParams = InvocationParameters.from(invocationContext);
            TokenStream tokenStream = agentService.plan(message, invocationParams);
            log.info("[Planner] token stream start, conversationId={}, taskId={}, parentToolCallId={}, timeoutSeconds={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    properties.getPlanner().getTimeoutSeconds());
            SubAgentDebugWriter.append("PlannerSubAgent", "token_stream_start", SubAgentDebugWriter.fields(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "parentToolCallId", parentToolCallId,
                    "timeoutSeconds", properties.getPlanner().getTimeoutSeconds()
            ));

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
                responseText = future.get(properties.getPlanner().getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("[Planner] timeout, conversationId={}, taskId={}, timeoutSeconds={}, partialResponseLength={}, partialResponsePreview={}, elapsedMs={}",
                        conversationId,
                        taskId,
                        properties.getPlanner().getTimeoutSeconds(),
                        fullResponse.length(),
                        preview(fullResponse.toString()),
                        System.currentTimeMillis() - startTime,
                        e);
                SubAgentDebugWriter.append("PlannerSubAgent", "timeout", SubAgentDebugWriter.fields(
                        "taskId", taskId,
                        "conversationId", conversationId,
                        "timeoutSeconds", properties.getPlanner().getTimeoutSeconds(),
                        "partialResponseLength", fullResponse.length(),
                        "partialResponsePreview", preview(fullResponse.toString()),
                        "elapsedMs", System.currentTimeMillis() - startTime
                ));
                throw e;
            }
            log.info("[Planner] response received, taskId={}, responseLength={}, responsePreview={}",
                    taskId,
                    StringUtils.length(responseText),
                    preview(responseText));
            SubAgentDebugWriter.append("PlannerSubAgent", "response_received", SubAgentDebugWriter.fields(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "responseLength", StringUtils.length(responseText),
                    "responsePreview", preview(responseText)
            ));
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
            SubAgentDebugWriter.append("PlannerSubAgent", "parse_success", SubAgentDebugWriter.fields(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "sqlBlockCount", CollectionUtils.size(plan.getSqlBlocks()),
                    "planStepCount", CollectionUtils.size(plan.getPlanSteps()),
                    "summaryLength", StringUtils.length(plan.getSummaryText()),
                    "rawResponseLength", StringUtils.length(plan.getRawResponse()),
                    "summaryPreview", preview(plan.getSummaryText()),
                    "elapsedMs", System.currentTimeMillis() - startTime
            ));
            return plan;

        } catch (TimeoutException e) {
            observer.emitError(e.getMessage());
            SubAgentDebugWriter.append("PlannerSubAgent", "invoke_failed", SubAgentDebugWriter.fields(
                    "conversationId", conversationId,
                    "taskId", taskId,
                    "parentToolCallId", parentToolCallId,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e),
                    "instructionPreview", preview(request.getInstruction())
            ));
            throw new RuntimeException("Planner SubAgent timed out: " + e.getMessage(), e);
        } catch (Exception e) {
            observer.emitError(e.getMessage());
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
            SubAgentDebugWriter.append("PlannerSubAgent", "invoke_failed", SubAgentDebugWriter.fields(
                    "conversationId", conversationId,
                    "taskId", taskId,
                    "parentToolCallId", parentToolCallId,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e),
                    "instructionPreview", preview(request.getInstruction()),
                    "objectPreview", schemaSummary != null ? summarizeObjects(schemaSummary.getObjects()) : "[]"
            ));
            if (StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_calls")
                    && StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_call_id")) {
                SubAgentDebugWriter.append("PlannerSubAgent", "protocol_error_hint", SubAgentDebugWriter.fields(
                        "taskId", taskId,
                        "conversationId", conversationId,
                        "hint", "assistant tool_calls were emitted but matching tool messages were not present in the next model request",
                        "nextCheck", "inspect planner-side message assembly or tool-result replay for this task"
                ));
            }
            throw new RuntimeException("Planner SubAgent failed: " + e.getMessage(), e);
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
                if (StringUtils.isNotBlank(object.getRelevance())) {
                    sb.append(" {relevance=").append(object.getRelevance()).append("}");
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

    private Long resolveConversationId() {
        try {
            Long id = RequestContext.getConversationId();
            return id != null ? id : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String resolveModelName() {
        String modelName = AgentRequestContext.getModelName();
        if (StringUtils.isNotBlank(modelName)) {
            return modelName;
        }
        log.warn("No modelName in AgentRequestContext, falling back to default");
        return "qwen3-max";
    }

    private int countDdlObjects(SchemaSummary summary) {
        if (summary == null || CollectionUtils.isEmpty(summary.getObjects())) {
            return 0;
        }
        return (int) summary.getObjects().stream()
                .filter(object -> StringUtils.isNotBlank(object.getObjectDdl()))
                .count();
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
}
