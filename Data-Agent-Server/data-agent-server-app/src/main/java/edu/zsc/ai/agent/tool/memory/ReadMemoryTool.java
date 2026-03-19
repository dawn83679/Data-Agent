package edu.zsc.ai.agent.tool.memory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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

    @Tool({
            "Value: recalls durable memory across conversation, workspace, and user dimensions when prompt-injected memory is not enough.",
            "Use When: only when the current task needs targeted durable context that is missing or unclear in the current prompt.",
            "Preconditions: intent should describe the durable fact you need; optional scope, memoryType, and subType can narrow the recall.",
            "After Success: use only the returned durable memory that directly helps the task; do not narrate the tool call itself to the user.",
            "After Failure: refine the intent or filters and retry only if durable context is still needed.",
            "Do Not Use When: prompt-injected memory already covers the need, or when you just need transient turn context."
    })
    public AgentToolResult readMemory(
            @P("What durable context you want to recall") String intent,
            @P(value = "Optional scope filter: USER/WORKSPACE/CONVERSATION", required = false) String scope,
            @P(value = "Optional memory type filter", required = false) String memoryType,
            @P(value = "Optional memory subType filter", required = false) String subType) {

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

    private String normalizeScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return null;
        }
        MemoryScopeEnum memoryScope = MemoryScopeEnum.fromCode(scope);
        if (memoryScope == null) {
            throw new IllegalArgumentException("Unsupported scope '" + scope + "'. Valid values: " + MemoryScopeEnum.validCodes());
        }
        return memoryScope.getCode();
    }

    private String normalizeMemoryType(String memoryType) {
        if (StringUtils.isBlank(memoryType)) {
            return null;
        }
        MemoryTypeEnum type = MemoryTypeEnum.fromCode(memoryType);
        if (type == null) {
            throw new IllegalArgumentException(
                    "Unsupported memoryType '" + memoryType + "'. Valid values: " + MemoryTypeEnum.validCodes());
        }
        return type.getCode();
    }

    private String normalizeSubType(String memoryType, String subType) {
        if (StringUtils.isBlank(subType)) {
            return null;
        }
        MemorySubTypeEnum parsed = MemorySubTypeEnum.fromCode(subType);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unsupported subType '" + subType + "'. Valid values: " + MemorySubTypeEnum.validCodes());
        }
        if (StringUtils.isNotBlank(memoryType) && !parsed.belongsTo(MemoryTypeEnum.fromCode(memoryType))) {
            throw new IllegalArgumentException(
                    "subType '" + subType + "' does not belong to memoryType '" + memoryType + "'.");
        }
        return parsed.getCode();
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
        payload.put(MemoryRecallConstant.ITEM_WORKSPACE_LEVEL, item.getWorkspaceLevel());
        payload.put(MemoryRecallConstant.ITEM_WORKSPACE_CONTEXT_KEY, item.getWorkspaceContextKey());
        payload.put(MemoryRecallConstant.ITEM_MEMORY_TYPE, item.getMemoryType());
        payload.put(MemoryRecallConstant.ITEM_SUB_TYPE, item.getSubType());
        payload.put(MemoryRecallConstant.ITEM_TITLE, item.getTitle());
        payload.put(MemoryRecallConstant.ITEM_CONTENT, item.getContent());
        payload.put(MemoryRecallConstant.ITEM_REASON, item.getReason());
        payload.put(MemoryRecallConstant.ITEM_REVIEW_STATE, item.getReviewState());
        payload.put(MemoryRecallConstant.ITEM_SOURCE_TYPE, item.getSourceType());
        payload.put(MemoryRecallConstant.ITEM_SCORE, item.getScore());
        return payload;
    }
}
