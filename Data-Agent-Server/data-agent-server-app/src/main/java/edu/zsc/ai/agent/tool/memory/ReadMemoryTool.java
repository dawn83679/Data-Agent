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
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
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
            "价值：读取可能改变当前决策的持久记忆。",
            "使用时机：缺少或仍不明确稳定偏好、业务规则、字段定义、对象事实或可复用 SQL 模式。",
            "前置条件：intent 必须明确说明要确认或检索什么。",
            "结果：返回召回的记忆、命中过滤条件和摘要。",
            "边界：不要为了临时任务指令、本轮情绪或提示词里已经明确的事实调用它；只使用对当前任务有实质帮助的记忆。"
    })
    public AgentToolResult readMemory(
            @P("你要召回的持久上下文") String intent,
            @P(value = "可选记忆范围过滤：USER 或 CONVERSATION。", required = false) MemoryScopeEnum scope,
            @P(value = "可选记忆类型过滤。有效值：PREFERENCE、BUSINESS_RULE、KNOWLEDGE_POINT、WORKFLOW_CONSTRAINT、GOLDEN_SQL_CASE。", required = false) MemoryTypeEnum memoryType,
            @P(value = "可选记忆子类型过滤。必须精确匹配枚举值，例如 RESPONSE_FORMAT、LANGUAGE_PREFERENCE、PRODUCT_RULE、DOMAIN_RULE、GOVERNANCE_RULE、SAFETY_RULE、ARCHITECTURE_KNOWLEDGE、DOMAIN_KNOWLEDGE、GLOSSARY、OBJECT_KNOWLEDGE、PROCESS_RULE、APPROVAL_RULE、IMPLEMENTATION_CONSTRAINT、REVIEW_CONSTRAINT、QUERY_PATTERN、JOIN_STRATEGY、VALIDATED_SQL、METRIC_CALCULATION。", required = false) MemorySubTypeEnum subType,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (!isAllowedAgent(agentType, agentMode)) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "readMemory 只允许主 Agent 或记忆写入 Agent 在普通执行模式下使用。",
                    "不要在当前 Agent 上下文中尝试召回记忆。"
            ));
        }
        if (StringUtils.isBlank(intent)) {
            return AgentToolResult.fail("readMemory 必须提供 intent。");
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
                    "只使用对当前任务有实质帮助的持久记忆。"
            ));
        } catch (IllegalArgumentException e) {
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("readMemory failed", e);
            return AgentToolResult.fail("召回记忆失败。重试前请缩小或修正召回 intent 和过滤条件。");
        }
    }

    public AgentToolResult readMemory(
            String intent,
            MemoryScopeEnum scope,
            MemoryTypeEnum memoryType,
            MemorySubTypeEnum subType,
            InvocationParameters parameters) {
        return readMemory(intent, scope, memoryType, subType, null, parameters);
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
                memory == null ? "没有找到当前会话工作记忆。"
                        : "已加载当前会话工作记忆。");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(MemoryRecallConstant.RESULT_SUMMARY,
                memory == null ? "没有找到当前会话工作记忆。"
                        : "已加载当前会话工作记忆。");
        result.put(MemoryRecallConstant.RESULT_APPLIED_FILTERS, Map.of(
                MemoryRecallConstant.ITEM_SCOPE, MemoryScopeEnum.CONVERSATION.getCode(),
                MemoryRecallConstant.ITEM_MEMORY_TYPE, MemoryTypeEnum.WORKFLOW_CONSTRAINT.getCode(),
                MemoryRecallConstant.ITEM_SUB_TYPE, MemorySubTypeEnum.CONVERSATION_WORKING_MEMORY.getCode()
        ));
        result.put(MemoryRecallConstant.RESULT_ITEMS,
                memory == null ? List.of() : List.of(toPayload(memory)));
        return AgentToolResult.success(result, ToolMessageSupport.sentence(
                memory == null ? "没有找到当前会话工作记忆。"
                        : "已加载当前会话工作记忆。",
                "只使用对当前任务有实质帮助的持久记忆。"
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
                    "记忆子类型 `" + subType.getCode() + "` 不属于记忆类型 `" + memoryType
                            + "`。有效记忆子类型：" + MemorySubTypeEnum.validCodesForText(MemoryTypeEnum.fromCode(memoryType)));
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
