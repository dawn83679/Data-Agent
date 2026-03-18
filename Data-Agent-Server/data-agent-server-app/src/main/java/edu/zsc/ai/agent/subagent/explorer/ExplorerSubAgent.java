package edu.zsc.ai.agent.subagent.explorer;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.AbstractSubAgent;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentPromptBuilder;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.util.ConnectionIdUtil;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Explorer SubAgent implementation.
 * Uses SearchObjectsTool and GetObjectDetailTool to explore schema and returns SchemaSummary.
 * Supports previousError for supplementary exploration. connectionIds: 1 = single Explorer, 2 = concurrent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplorerSubAgent extends AbstractSubAgent<SubAgentRequest, SchemaSummary> implements SubAgent<SubAgentRequest, SchemaSummary> {

    private final SubAgentFactory subAgentFactory;
    private final SubAgentProperties properties;
    private final SubAgentStreamBridge streamBridge;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final AgentLogService agentLogService;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.EXPLORER;
    }

    @Override
    public SchemaSummary invoke(SubAgentRequest request) {
        long startTime = System.currentTimeMillis();
        Long requestedTimeoutSeconds = request.timeoutSeconds();
        long timeoutSeconds = resolveTimeoutSeconds(requestedTimeoutSeconds, properties.getExplorer().getTimeoutSeconds());
        Long conversationId = resolveConversationId();
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        String taskId = AgentExecutionContext.getTaskId();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        String modelName = resolveModelName(log);
        log.info("[Explorer] invoke start, conversationId={}, taskId={}, parentToolCallId={}, modelName={}, connectionIds={}, requestedTimeoutSeconds={}, effectiveTimeoutSeconds={}, hasRequestContext={}, hasAgentContext={}, hasContext={}, instructionLength={}, contextLength={}, instructionPreview={}, contextPreview={}",
                requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                taskId,
                parentToolCallId,
                modelName,
                request.connectionIds(),
                requestedTimeoutSeconds,
                timeoutSeconds,
                requestContextSnapshot != null,
                AgentRequestContext.hasContext(),
                StringUtils.isNotBlank(request.context()),
                StringUtils.length(request.instruction()),
                StringUtils.length(request.context()),
                preview(request.instruction()),
                preview(request.context()));
        agentLogService.recordDebug("ExplorerSubAgent", "invoke_start", AgentLogFields.of(
                "conversationId", requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : conversationId,
                "taskId", taskId,
                "parentToolCallId", parentToolCallId,
                "modelName", modelName,
                "connectionIds", request.connectionIds(),
                "requestedTimeoutSeconds", requestedTimeoutSeconds,
                "effectiveTimeoutSeconds", timeoutSeconds,
                "instructionPreview", preview(request.instruction()),
                "contextPreview", preview(request.context())
        ));

        // SSE progress emitter (replaces AgentListener-based observability)
        SubAgentObservabilityListener observer = new SubAgentObservabilityListener(
                AgentTypeEnum.EXPLORER, conversationId, sseEmitterRegistry, null, taskId, parentToolCallId,
                CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null,
                timeoutSeconds,
                agentLogService);

        observer.emitStart();

        try {
            String message = buildMessage(request);
            log.info("[Explorer] message built, taskId={}, messageLength={}, messagePreview={}",
                    taskId,
                    StringUtils.length(message),
                    preview(message));
            String systemPrompt = PromptConfig.getPrompt(PromptEnum.EXPLORER);

            ExplorerAgentService agentService = subAgentFactory.buildExplorerAgent(
                    modelName, systemPrompt);

            Map<String, Object> invocationContext = buildInvocationContext(request);
            log.info("[Explorer] invocation context built, taskId={}, defaultConnectionId={}, allowedConnectionIds={}, invocationKeys={}",
                    taskId,
                    CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null,
                    request.connectionIds(),
                    invocationContext.keySet());
            agentLogService.recordDebug("ExplorerSubAgent", "model_request_ready", AgentLogFields.of(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "parentToolCallId", parentToolCallId,
                    "modelName", modelName,
                    "systemPromptLength", StringUtils.length(systemPrompt),
                    "systemPromptPreview", preview(systemPrompt),
                    "messageLength", StringUtils.length(message),
                    "messagePreview", preview(message),
                    "defaultConnectionId", CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null,
                    "allowedConnectionIds", request.connectionIds(),
                    "invocationKeys", invocationContext.keySet(),
                    "messageProtocolHint", "agentService API does not expose raw tool-call messages before dispatch"
            ));
            InvocationParameters invocationParams = InvocationParameters.from(invocationContext);
            TokenStream tokenStream = agentService.explore(message, invocationParams);

            StringBuilder fullResponse = new StringBuilder();
            String parentId = parentToolCallId;
            Sinks.Many<ChatResponseBlock> sink = sseEmitterRegistry.get(conversationId).orElse(null);
            streamBridge.bridge(tokenStream, sink, parentId, null, fullResponse::append);

            CompletableFuture<String> future = new CompletableFuture<>();
            tokenStream.onCompleteResponse(response -> future.complete(fullResponse.toString()));
            tokenStream.onError(error -> future.completeExceptionally(error));
            log.info("[Explorer] token stream start, conversationId={}, taskId={}, parentToolCallId={}, timeoutSeconds={}",
                    conversationId,
                    taskId,
                    parentId,
                    timeoutSeconds);
            agentLogService.recordDebug("ExplorerSubAgent", "token_stream_start", AgentLogFields.of(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "parentToolCallId", parentId,
                    "timeoutSeconds", timeoutSeconds
            ));
            tokenStream.start();

            String responseText;
            try {
                responseText = future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                log.warn("[Explorer] timeout, conversationId={}, taskId={}, timeoutSeconds={}, partialResponseLength={}, partialResponsePreview={}, elapsedMs={}",
                        conversationId,
                        taskId,
                        timeoutSeconds,
                        fullResponse.length(),
                        preview(fullResponse.toString()),
                        System.currentTimeMillis() - startTime,
                        te);
                agentLogService.recordDebug("ExplorerSubAgent", "timeout", AgentLogFields.of(
                        "taskId", taskId,
                        "conversationId", conversationId,
                        "timeoutSeconds", timeoutSeconds,
                        "partialResponseLength", fullResponse.length(),
                        "partialResponsePreview", preview(fullResponse.toString()),
                        "elapsedMs", System.currentTimeMillis() - startTime
                ));
                throw te;
            }

            log.info("[Explorer] response received, taskId={}, responseLength={}, responsePreview={}",
                    taskId,
                    StringUtils.length(responseText),
                    preview(responseText));
            agentLogService.recordDebug("ExplorerSubAgent", "response_received", AgentLogFields.of(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "responseLength", StringUtils.length(responseText),
                    "responsePreview", preview(responseText)
            ));
            SchemaSummary summary = ExplorerResponseParser.parse(responseText);
            observer.emitComplete(summary.getSummaryText(), JsonUtil.object2json(summary));
            log.info("[Explorer] parse success, taskId={}, objectCount={}, summaryLength={}, rawResponseLength={}, summaryPreview={}, objectPreview={}, elapsedMs={}",
                    taskId,
                    CollectionUtils.size(summary.getObjects()),
                    StringUtils.length(summary.getSummaryText()),
                    StringUtils.length(summary.getRawResponse()),
                    preview(summary.getSummaryText()),
                    summarizeObjects(summary.getObjects()),
                    System.currentTimeMillis() - startTime);
            agentLogService.recordDebug("ExplorerSubAgent", "parse_success", AgentLogFields.of(
                    "taskId", taskId,
                    "conversationId", conversationId,
                    "objectCount", CollectionUtils.size(summary.getObjects()),
                    "summaryLength", StringUtils.length(summary.getSummaryText()),
                    "rawResponseLength", StringUtils.length(summary.getRawResponse()),
                    "summaryPreview", preview(summary.getSummaryText()),
                    "objectPreview", summarizeObjects(summary.getObjects()),
                    "elapsedMs", System.currentTimeMillis() - startTime
            ));
            return summary;

        } catch (Exception e) {
            String errorSummary = errorSummary(e, "Explorer SubAgent failed", timeoutSeconds);
            observer.emitError(errorSummary);
            log.error("[Explorer] invoke failed, conversationId={}, taskId={}, parentToolCallId={}, elapsedMs={}, rootCauseClass={}, rootCauseMessage={}",
                    conversationId,
                    taskId,
                    parentToolCallId,
                    System.currentTimeMillis() - startTime,
                    rootCause(e).getClass().getSimpleName(),
                    rootCauseMessage(e),
                    e);
            agentLogService.recordDebug("ExplorerSubAgent", "invoke_failed", AgentLogFields.of(
                    "conversationId", conversationId,
                    "taskId", taskId,
                    "parentToolCallId", parentToolCallId,
                    "elapsedMs", System.currentTimeMillis() - startTime,
                    "rootCauseClass", rootCause(e).getClass().getSimpleName(),
                    "rootCauseMessage", rootCauseMessage(e),
                    "instructionPreview", preview(request.instruction()),
                    "contextPreview", preview(request.context())
            ));
            if (StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_calls")
                    && StringUtils.containsIgnoreCase(rootCauseMessage(e), "tool_call_id")) {
                agentLogService.recordDebug("ExplorerSubAgent", "protocol_error_hint", AgentLogFields.of(
                        "taskId", taskId,
                        "conversationId", conversationId,
                        "hint", "assistant tool_calls were emitted but matching tool messages were not present in the next model request",
                        "nextCheck", "inspect message assembly around streamBridge/tool-result replay for this task"
                ));
            }
            throw new RuntimeException("Explorer SubAgent failed: " + errorSummary, e);
        }
    }

    private String buildMessage(SubAgentRequest request) {
        Long connectionId = CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null;
        return SubAgentPromptBuilder.builder()
                .instruction(request.instruction())
                .connectionId(connectionId)
                .allowedConnectionIds(request.connectionIds())
                .context(request.context())
                .build();
    }

    private Map<String, Object> buildInvocationContext(SubAgentRequest request) {
        Map<String, Object> invocationContext = createInvocationContext(AgentTypeEnum.EXPLORER);
        List<Long> allowedConnectionIds = request.connectionIds();
        Long defaultConnectionId = CollectionUtils.isNotEmpty(allowedConnectionIds) ? allowedConnectionIds.get(0) : null;

        if (CollectionUtils.isNotEmpty(allowedConnectionIds)) {
            invocationContext.put(InvocationContextConstant.ALLOWED_CONNECTION_IDS, ConnectionIdUtil.toCsv(allowedConnectionIds));
            invocationContext.put(InvocationContextConstant.CONNECTION_ID, defaultConnectionId);
        } else {
            invocationContext.remove(InvocationContextConstant.ALLOWED_CONNECTION_IDS);
            invocationContext.remove(InvocationContextConstant.CONNECTION_ID);
        }
        return invocationContext;
    }

}
