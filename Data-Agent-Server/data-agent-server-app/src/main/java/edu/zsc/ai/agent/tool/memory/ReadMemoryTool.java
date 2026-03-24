package edu.zsc.ai.agent.tool.memory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.MemoryRecallConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallContext;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallManager;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallMode;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadMemoryTool {

    private final MemoryRecallManager memoryRecallManager;
    private final MemoryService memoryService;
    private static final String MEMORY_TYPE_GUIDE =
            "PREFERENCE (RESPONSE_FORMAT, LANGUAGE_PREFERENCE); "
                    + "BUSINESS_RULE (PRODUCT_RULE, DOMAIN_RULE, GOVERNANCE_RULE, SAFETY_RULE); "
                    + "KNOWLEDGE_POINT (ARCHITECTURE_KNOWLEDGE, DOMAIN_KNOWLEDGE, GLOSSARY, OBJECT_KNOWLEDGE); "
                    + "WORKFLOW_CONSTRAINT (PROCESS_RULE, APPROVAL_RULE, IMPLEMENTATION_CONSTRAINT, REVIEW_CONSTRAINT); "
                    + "GOLDEN_SQL_CASE (QUERY_PATTERN, JOIN_STRATEGY, VALIDATED_SQL, METRIC_CALCULATION).";

    @Tool({
            "Value: recalls durable memory across conversation and user dimensions when prompt-injected memory is not enough.",
            "Use When: call when the current task depends on durable context that is missing, insufficient, or ambiguous in the current prompt.",
            "Common Cases: checking a stable user preference, a durable workflow rule, or a reusable domain rule before deciding how to answer.",
            "Preconditions: intent must describe the durable context you want; optional scope, memoryType, and subType should narrow recall to the smallest useful slice.",
            "Scope Guidance: prefer the narrowest valid scope. Use USER for cross-conversation durable memory and CONVERSATION only for short-lived but reusable context within this conversation.",
            "Classification Guidance: use memoryType/subType only when you already know the likely class of memory. If unsure, leave filters empty rather than guessing wrong.",
            "Valid Classes: " + MEMORY_TYPE_GUIDE,
            "Subtype Rule: subType must be one of the exact uppercase values above. Do not invent labels such as DATABASE_SCHEMA. If unsure, omit subType.",
            "After Success: use only the returned durable memory that directly helps the task; do not narrate the tool call itself to the user.",
            "After Failure: refine the intent or filters and retry only if durable context is still needed.",
            "Do Not Use When: prompt-injected memory already covers the need, or when you only need transient turn context, current-turn emotions, or one-off task instructions."
    })
    public AgentToolResult readMemory(
            @P("What durable context you want to recall") String intent,
            @P(value = "Optional scope filter: USER or CONVERSATION", required = false) MemoryScopeEnum scope,
            @P(value = "Optional memory type filter. Valid values: PREFERENCE, BUSINESS_RULE, KNOWLEDGE_POINT, WORKFLOW_CONSTRAINT, GOLDEN_SQL_CASE", required = false) MemoryTypeEnum memoryType,
            @P(value = "Optional memory subType filter. Must exactly match one of: RESPONSE_FORMAT, LANGUAGE_PREFERENCE, PRODUCT_RULE, DOMAIN_RULE, GOVERNANCE_RULE, SAFETY_RULE, ARCHITECTURE_KNOWLEDGE, DOMAIN_KNOWLEDGE, GLOSSARY, OBJECT_KNOWLEDGE, PROCESS_RULE, APPROVAL_RULE, IMPLEMENTATION_CONSTRAINT, REVIEW_CONSTRAINT, QUERY_PATTERN, JOIN_STRATEGY, VALIDATED_SQL, METRIC_CALCULATION", required = false) MemorySubTypeEnum subType,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (agentType != AgentTypeEnum.MAIN || agentMode != AgentModeEnum.AGENT) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "readMemory is only available to the main agent in normal execution mode.",
                    "Do not attempt to recall memory from this agent context."
            ));
        }
        if (StringUtils.isBlank(intent)) {
            return AgentToolResult.fail("intent is required for readMemory.");
        }

        try {
            String normalizedScope = normalizeScope(scope);
            String normalizedMemoryType = normalizeMemoryType(memoryType);
            String normalizedSubType = normalizeSubType(normalizedMemoryType, subType);

            MemoryRecallResult recallResult = memoryRecallManager.recall(MemoryRecallContext.builder()
                    .conversationId(RequestContext.getConversationId())
                    .queryText(intent)
                    .scope(normalizedScope)
                    .memoryType(resolveRecallMemoryType(normalizedMemoryType, normalizedSubType))
                    .subType(normalizedSubType)
                    .recallMode(MemoryRecallMode.TOOL)
                    .minScore(0.0D)
                    .build());

            List<Long> memoryIds = recallResult.getItems().stream()
                    .map(MemoryRecallItem::getId)
                    .toList();
            memoryService.recordMemoryAccess(memoryIds);
            log.info("[Tool done] readMemory, conversationId={}, scope={}, memoryType={}, subType={}, recalledCount={}, memoryIds={}, summary={}",
                    RequestContext.getConversationId(),
                    normalizedScope,
                    resolveRecallMemoryType(normalizedMemoryType, normalizedSubType),
                    normalizedSubType,
                    memoryIds.size(),
                    memoryIds,
                    recallResult.getSummary());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put(MemoryRecallConstant.RESULT_SUMMARY, recallResult.getSummary());
            result.put(MemoryRecallConstant.RESULT_APPLIED_FILTERS, recallResult.getAppliedFilters());
            result.put(MemoryRecallConstant.RESULT_ITEMS, recallResult.getItems().stream().map(this::toPayload).toList());

            return AgentToolResult.success(result, ToolMessageSupport.sentence(
                    recallResult.getSummary(),
                    "Use only the recalled durable memory that materially helps the current task."
            ));
        } catch (IllegalArgumentException e) {
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("readMemory failed", e);
            return AgentToolResult.fail("Failed to recall memory. Refine the recall intent or filters before retrying.");
        }
    }

    private String normalizeScope(MemoryScopeEnum scope) {
        if (scope == null) {
            return null;
        }
        return scope.getCode();
    }

    private String normalizeMemoryType(MemoryTypeEnum memoryType) {
        if (memoryType == null) {
            return null;
        }
        return memoryType.getCode();
    }

    private String normalizeSubType(String memoryType, MemorySubTypeEnum subType) {
        if (subType == null) {
            return null;
        }
        if (StringUtils.isNotBlank(memoryType) && !subType.belongsTo(MemoryTypeEnum.fromCode(memoryType))) {
            throw new IllegalArgumentException(
                    "subType '" + subType.getCode() + "' does not belong to memoryType '" + memoryType
                            + "'. Valid subTypes: " + MemorySubTypeEnum.validCodesForText(MemoryTypeEnum.fromCode(memoryType)));
        }
        return subType.getCode();
    }

    private String resolveRecallMemoryType(String memoryType, String subType) {
        if (StringUtils.isNotBlank(memoryType)) {
            return memoryType;
        }
        if (StringUtils.isBlank(subType)) {
            return null;
        }
        MemorySubTypeEnum parsed = MemorySubTypeEnum.fromCode(subType);
        return parsed == null ? null : parsed.getMemoryType().getCode();
    }

    private Map<String, Object> toPayload(MemoryRecallItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MemoryRecallConstant.ITEM_MEMORY_ID, item.getId());
        payload.put(MemoryRecallConstant.ITEM_SCOPE, item.getScope());
        payload.put(MemoryRecallConstant.ITEM_MEMORY_TYPE, item.getMemoryType());
        payload.put(MemoryRecallConstant.ITEM_SUB_TYPE, item.getSubType());
        payload.put(MemoryRecallConstant.ITEM_TITLE, item.getTitle());
        payload.put(MemoryRecallConstant.ITEM_CONTENT, item.getContent());
        payload.put(MemoryRecallConstant.ITEM_REASON, item.getReason());
        payload.put(MemoryRecallConstant.ITEM_SOURCE_TYPE, item.getSourceType());
        payload.put(MemoryRecallConstant.ITEM_SCORE, item.getScore());
        return payload;
    }
}
