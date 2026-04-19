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
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
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
    @Tool({
            "Use this tool only to fetch durable memory that may change the current decision.",
            "Call it when a stable preference, business rule, field definition, object fact, or reusable SQL pattern is missing or still ambiguous.",
            "Do not call it for temporary task instructions, current-turn emotions, or facts already clear in the prompt.",
            "intent must say exactly what you want to confirm or retrieve.",
            "Add scope, memoryType, or subType only when you already know them; otherwise leave them empty instead of guessing.",
            "If you plan to UPDATE or DELETE memory, usually call readMemory first to find the target memoryId.",
            "After reading, use only the returned memory that materially helps the task. Ignore irrelevant recalls."
    })
    public AgentToolResult readMemory(
            @P("What durable context you want to recall") String intent,
            @P(value = "Optional scope filter: USER or CONVERSATION", required = false) MemoryScopeEnum scope,
            @P(value = "Optional memory type filter. Valid values: PREFERENCE, BUSINESS_RULE, KNOWLEDGE_POINT, WORKFLOW_CONSTRAINT, GOLDEN_SQL_CASE", required = false) MemoryTypeEnum memoryType,
            @P(value = "Optional memory subType filter. Must exactly match one of: RESPONSE_FORMAT, LANGUAGE_PREFERENCE, PRODUCT_RULE, DOMAIN_RULE, GOVERNANCE_RULE, SAFETY_RULE, ARCHITECTURE_KNOWLEDGE, DOMAIN_KNOWLEDGE, GLOSSARY, OBJECT_KNOWLEDGE, PROCESS_RULE, APPROVAL_RULE, IMPLEMENTATION_CONSTRAINT, REVIEW_CONSTRAINT, QUERY_PATTERN, JOIN_STRATEGY, VALIDATED_SQL, METRIC_CALCULATION", required = false) MemorySubTypeEnum subType,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (!isAllowedAgent(agentType, agentMode)) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "readMemory is only available to the main agent or memory writer in normal execution mode.",
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

            if (isConversationWorkingMemoryLookup(normalizedScope, normalizedMemoryType, normalizedSubType)) {
                return directConversationWorkingMemoryResult();
            }

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

    private boolean isAllowedAgent(AgentTypeEnum agentType, AgentModeEnum agentMode) {
        return agentMode == AgentModeEnum.AGENT
                && (agentType == AgentTypeEnum.MAIN || agentType == AgentTypeEnum.MEMORY_WRITER);
    }

    private boolean isConversationWorkingMemoryLookup(String scope, String memoryType, String subType) {
        return MemoryScopeEnum.CONVERSATION.matches(scope)
                && MemoryTypeEnum.WORKFLOW_CONSTRAINT.matches(memoryType)
                && MemorySubTypeEnum.CONVERSATION_WORKING_MEMORY.getCode().equals(subType);
    }

    private AgentToolResult directConversationWorkingMemoryResult() {
        Long userId = RequestContext.getUserId();
        Long conversationId = RequestContext.getConversationId();
        AiMemory memory = memoryService.getConversationWorkingMemory(userId, conversationId);
        List<Long> memoryIds = memory == null ? List.of() : List.of(memory.getId());
        memoryService.recordMemoryAccess(memoryIds);
        log.info("[Tool done] readMemory, conversationId={}, scope={}, memoryType={}, subType={}, recalledCount={}, memoryIds={}, summary={}",
                conversationId,
                MemoryScopeEnum.CONVERSATION.getCode(),
                MemoryTypeEnum.WORKFLOW_CONSTRAINT.getCode(),
                MemorySubTypeEnum.CONVERSATION_WORKING_MEMORY.getCode(),
                memoryIds.size(),
                memoryIds,
                memory == null ? "No current conversation working memory found."
                        : "Loaded the current conversation working memory.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(MemoryRecallConstant.RESULT_SUMMARY,
                memory == null ? "No current conversation working memory found."
                        : "Loaded the current conversation working memory.");
        result.put(MemoryRecallConstant.RESULT_APPLIED_FILTERS, Map.of(
                MemoryRecallConstant.ITEM_SCOPE, MemoryScopeEnum.CONVERSATION.getCode(),
                MemoryRecallConstant.ITEM_MEMORY_TYPE, MemoryTypeEnum.WORKFLOW_CONSTRAINT.getCode(),
                MemoryRecallConstant.ITEM_SUB_TYPE, MemorySubTypeEnum.CONVERSATION_WORKING_MEMORY.getCode()
        ));
        result.put(MemoryRecallConstant.RESULT_ITEMS,
                memory == null ? List.of() : List.of(toPayload(memory)));
        return AgentToolResult.success(result, ToolMessageSupport.sentence(
                memory == null ? "No current conversation working memory found."
                        : "Loaded the current conversation working memory.",
                "Use only the recalled durable memory that materially helps the current task."
        ));
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

    private Map<String, Object> toPayload(AiMemory memory) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MemoryRecallConstant.ITEM_MEMORY_ID, memory.getId());
        payload.put(MemoryRecallConstant.ITEM_SCOPE, memory.getScope());
        payload.put(MemoryRecallConstant.ITEM_MEMORY_TYPE, memory.getMemoryType());
        payload.put(MemoryRecallConstant.ITEM_SUB_TYPE, memory.getSubType());
        payload.put(MemoryRecallConstant.ITEM_TITLE, memory.getTitle());
        payload.put(MemoryRecallConstant.ITEM_CONTENT, memory.getContent());
        payload.put(MemoryRecallConstant.ITEM_REASON, memory.getReason());
        payload.put(MemoryRecallConstant.ITEM_SOURCE_TYPE, memory.getSourceType());
        payload.put(MemoryRecallConstant.ITEM_SCORE, 1.0D);
        return payload;
    }
}
