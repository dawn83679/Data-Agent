package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;
<<<<<<< HEAD
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
=======
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.observability.AgentLogFields;
import edu.zsc.ai.observability.AgentLogService;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MemoryRecallManager {

<<<<<<< HEAD
    private static final Logger runtimeLog = LoggerFactory.getLogger(MemoryRecallLogConstant.LOGGER_NAME);

    private final MemoryRecallQueryPlanner queryPlanner;
    private final MemoryRecallHandlerChain handlerChain;
    private final MemoryRecallPostProcessor postProcessor;

    public MemoryRecallManager(MemoryRecallQueryPlanner queryPlanner,
                               MemoryRecallHandlerChain handlerChain,
                               MemoryRecallPostProcessor postProcessor) {
        this.queryPlanner = queryPlanner;
        this.handlerChain = handlerChain;
        this.postProcessor = postProcessor;
=======
    private final MemoryRecallQueryPlanner queryPlanner;
    private final MemoryRecallHandlerChain handlerChain;
    private final MemoryRecallPostProcessor postProcessor;
    private final AgentLogService agentLogService;

    public MemoryRecallManager(MemoryRecallQueryPlanner queryPlanner,
                               MemoryRecallHandlerChain handlerChain,
                               MemoryRecallPostProcessor postProcessor,
                               AgentLogService agentLogService) {
        this.queryPlanner = queryPlanner;
        this.handlerChain = handlerChain;
        this.postProcessor = postProcessor;
        this.agentLogService = agentLogService;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    public MemoryRecallResult recall(MemoryRecallContext input) {
        if (input == null) {
            return MemoryRecallResult.empty();
        }

        MemoryRecallContext context = normalize(input);
        recordRecallStart(context);
        List<MemoryRecallQuery> queries = queryPlanner.plan(context);
        recordPlannedQueries(context, queries);
        if (log.isDebugEnabled()) {
            log.debug("Memory recall planned queries: conversationId={}, recallMode={}, queries={}",
                    context.getConversationId(),
                    context.getRecallMode(),
                    summarizeQueries(queries));
        }
        List<MemoryRecallItem> merged = queries.stream()
                .peek(this::recordQueryDispatch)
                .flatMap(query -> handlerChain.handle(query).stream())
                .toList();
        recordPrePostProcess(context, merged);

        MemoryRecallResult result = postProcessor.process(context, merged);
        recordRecallComplete(context, result);
        return result;
    }

    private MemoryRecallContext normalize(MemoryRecallContext input) {
        return input.toBuilder()
                .scope(normalizeToken(input.getScope()))
                .memoryType(normalizeToken(input.getMemoryType()))
                .subType(normalizeToken(input.getSubType()))
                .build();
    }

    private String normalizeToken(String value) {
        return StringUtils.isBlank(value) ? null : value.trim().toUpperCase();
    }

    private void recordRecallStart(MemoryRecallContext context) {
<<<<<<< HEAD
        runtimeLog.info("{} recallMode={} queryText={} requestedScope={} requestedMemoryType={} requestedSubType={}",
                MemoryRecallLogConstant.EVENT_RECALL_START,
                context.getRecallMode() == null ? null : context.getRecallMode().name(),
                context.getQueryText(),
                context.getScope(),
                context.getMemoryType(),
                context.getSubType());
    }

    private void recordPlannedQueries(MemoryRecallContext context, List<MemoryRecallQuery> queries) {
        runtimeLog.info("{} recallMode={} plannedQueries={}",
                MemoryRecallLogConstant.EVENT_RECALL_PLANNED,
                context.getRecallMode() == null ? null : context.getRecallMode().name(),
                summarizeQueries(queries));
    }

    private void recordQueryDispatch(MemoryRecallQuery query) {
        runtimeLog.info("{} queryName={} planningReason={} targetScope={} queryStrategy={} priority={}",
                MemoryRecallLogConstant.EVENT_RECALL_QUERY_DISPATCH,
                query.queryName(),
                query.planningReason(),
                query.targetScope(),
                query.queryStrategy() == null ? null : query.queryStrategy().name(),
                query.priority());
    }

    private void recordPrePostProcess(MemoryRecallContext context, List<MemoryRecallItem> merged) {
        runtimeLog.info("{} recallMode={} mergedCount={} resultMemoryIds={}",
                MemoryRecallLogConstant.EVENT_RECALL_POST_PROCESS,
                context.getRecallMode() == null ? null : context.getRecallMode().name(),
                merged.size(),
                merged.stream().map(MemoryRecallItem::getId).toList());
    }

    private void recordRecallComplete(MemoryRecallContext context, MemoryRecallResult result) {
        runtimeLog.info("{} recallMode={} finalCount={} summary={} appliedFilters={}",
                MemoryRecallLogConstant.EVENT_RECALL_COMPLETE,
                context.getRecallMode() == null ? null : context.getRecallMode().name(),
                result.getItems() == null ? 0 : result.getItems().size(),
                result.getSummary(),
                result.getAppliedFilters());
=======
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_START,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_RECALL_MODE, context.getRecallMode() == null ? null : context.getRecallMode().name(),
                        MemoryRecallLogConstant.FIELD_QUERY_TEXT, context.getQueryText(),
                        MemoryRecallLogConstant.FIELD_REQUESTED_SCOPE, context.getScope(),
                        MemoryRecallLogConstant.FIELD_REQUESTED_MEMORY_TYPE, context.getMemoryType(),
                        MemoryRecallLogConstant.FIELD_REQUESTED_SUB_TYPE, context.getSubType()
                ));
    }

    private void recordPlannedQueries(MemoryRecallContext context, List<MemoryRecallQuery> queries) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_PLANNED,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_RECALL_MODE, context.getRecallMode() == null ? null : context.getRecallMode().name(),
                        MemoryRecallLogConstant.FIELD_PLANNED_QUERIES, queries.stream().map(this::toQueryPayload).toList()
                ));
    }

    private void recordQueryDispatch(MemoryRecallQuery query) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_QUERY_DISPATCH,
                toQueryPayload(query));
    }

    private void recordPrePostProcess(MemoryRecallContext context, List<MemoryRecallItem> merged) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_POST_PROCESS,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_RECALL_MODE, context.getRecallMode() == null ? null : context.getRecallMode().name(),
                        MemoryRecallLogConstant.FIELD_MERGED_COUNT, merged.size(),
                        MemoryRecallLogConstant.FIELD_RESULT_MEMORY_IDS, merged.stream().map(MemoryRecallItem::getId).toList()
                ));
    }

    private void recordRecallComplete(MemoryRecallContext context, MemoryRecallResult result) {
        agentLogService.recordDebug(MemoryRecallLogConstant.LOGGER_NAME, MemoryRecallLogConstant.EVENT_RECALL_COMPLETE,
                AgentLogFields.of(
                        MemoryRecallLogConstant.FIELD_RECALL_MODE, context.getRecallMode() == null ? null : context.getRecallMode().name(),
                        MemoryRecallLogConstant.FIELD_FINAL_COUNT, result.getItems() == null ? 0 : result.getItems().size(),
                        MemoryRecallLogConstant.FIELD_SUMMARY, result.getSummary(),
                        MemoryRecallLogConstant.FIELD_APPLIED_FILTERS, result.getAppliedFilters()
                ));
    }

    private Map<String, Object> toQueryPayload(MemoryRecallQuery query) {
        return AgentLogFields.of(
                MemoryRecallLogConstant.FIELD_QUERY_NAME, query.queryName(),
                MemoryRecallLogConstant.FIELD_PLANNING_REASON, query.planningReason(),
                MemoryRecallLogConstant.FIELD_TARGET_SCOPE, query.targetScope(),
                MemoryRecallLogConstant.FIELD_QUERY_STRATEGY, query.queryStrategy() == null ? null : query.queryStrategy().name(),
                MemoryRecallLogConstant.FIELD_PRIORITY, query.priority()
        );
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }

    private String summarizeQueries(List<MemoryRecallQuery> queries) {
        return queries.stream()
                .map(query -> query.queryName()
                        + "{reason=" + query.planningReason()
                        + ",scope=" + query.targetScope()
                        + ",strategy=" + query.queryStrategy()
                        + ",priority=" + query.priority()
                        + "}")
                .collect(Collectors.joining(", "));
    }
}
