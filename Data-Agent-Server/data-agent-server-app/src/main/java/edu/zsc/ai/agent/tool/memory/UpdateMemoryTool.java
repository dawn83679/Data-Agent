package edu.zsc.ai.agent.tool.memory;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.MemoryRecallConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.MemoryOperationEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryMutationRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class UpdateMemoryTool {

    private final MemoryService memoryService;
    @Tool({
            "价值：写入、更新或删除会影响后续轮次的持久记忆。",
            "使用时机：需要保存稳定偏好、稳定规则、已验证事实、字段定义、对象知识或可复用 SQL 模式。",
            "前置条件：UPDATE 或 DELETE 通常先调用 readMemory 获取 memoryId；content 必须简短、事实化、可复用、足够具体。",
            "结果：返回记忆 ID、范围、记忆类型、记忆子类型和实际动作。",
            "边界：不要写入一次性请求、临时上下文、猜测或原始聊天措辞；不要向用户叙述内部记忆写入过程。"
    })
    public AgentToolResult updateMemory(
            @P("操作类型：CREATE、UPDATE 或 DELETE。") MemoryOperationEnum operation,
            @P(value = "目标 memoryId。UPDATE 和 DELETE 必填；CREATE 时省略。", required = false) Long memoryId,
            @P(value = "记忆范围：USER 或 CONVERSATION。CREATE 必填，UPDATE 可选，DELETE 忽略。", required = false) MemoryScopeEnum scope,
            @P(value = "记忆类型：PREFERENCE、BUSINESS_RULE、KNOWLEDGE_POINT、WORKFLOW_CONSTRAINT 或 GOLDEN_SQL_CASE。CREATE 必填，UPDATE 可选，DELETE 忽略。", required = false) MemoryTypeEnum memoryType,
            @P(value = "记忆子类型。必须精确匹配枚举值，例如 RESPONSE_FORMAT、LANGUAGE_PREFERENCE、PRODUCT_RULE、DOMAIN_RULE、GOVERNANCE_RULE、SAFETY_RULE、ARCHITECTURE_KNOWLEDGE、DOMAIN_KNOWLEDGE、GLOSSARY、OBJECT_KNOWLEDGE、PROCESS_RULE、APPROVAL_RULE、IMPLEMENTATION_CONSTRAINT、REVIEW_CONSTRAINT、QUERY_PATTERN、JOIN_STRATEGY、VALIDATED_SQL、METRIC_CALCULATION。CREATE 必填，UPDATE 可选，DELETE 忽略。", required = false) MemorySubTypeEnum subType,
            @P(value = "简短记忆标题。CREATE 或 UPDATE 可选，DELETE 忽略。", required = false) String title,
            @P(value = "权威持久记忆内容。CREATE 和 UPDATE 必填。若是对象级范围或具体数据库知识，已知时包含精确标识：connectionId、连接名、catalog/数据库、schema、对象/表/视图名。", required = false) String content,
            @P(value = "简短说明为什么要保留、修改或删除这条记忆。", required = false) String reason,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (agentMode != AgentModeEnum.AGENT
                || (agentType != AgentTypeEnum.MAIN && agentType != AgentTypeEnum.MEMORY_WRITER)) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "updateMemory 只允许主 Agent 或记忆写入 Agent 在普通执行模式下使用。",
                    "不要在当前 Agent 上下文中尝试变更记忆。"
            ));
        }

        try {
            MemoryWriteResult mutationResult = memoryService.mutateAgentMemory(MemoryMutationRequest.builder()
                    .operation(operation == null ? null : operation.getCode())
                    .memoryId(memoryId)
                    .scope(scope == null ? null : scope.getCode())
                    .memoryType(memoryType == null ? null : memoryType.getCode())
                    .subType(subType == null ? null : subType.getCode())
                    .title(title)
                    .content(content)
                    .reason(reason)
                    .build());
            AiMemory memory = mutationResult.getMemory();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put(MemoryRecallConstant.ITEM_MEMORY_ID, memory.getId());
            result.put(MemoryRecallConstant.ITEM_SCOPE, memory.getScope());
            result.put(MemoryRecallConstant.ITEM_MEMORY_TYPE, memory.getMemoryType());
            result.put(MemoryRecallConstant.ITEM_SUB_TYPE, memory.getSubType());
            String action = mutationResult.getAction() == null
                    ? MemoryToolActionEnum.UNKNOWN.getCode()
                    : mutationResult.getAction().getCode();
            result.put(MemoryRecallConstant.ITEM_ACTION, action);
            log.info("[Tool done] updateMemory, operation={}, action={}, memoryId={}, scope={}, memoryType={}, subType={}",
                    operation == null ? null : operation.getCode(),
                    action,
                    memory.getId(),
                    memory.getScope(),
                    memory.getMemoryType(),
                    memory.getSubType());
            return AgentToolResult.success(result, ToolMessageSupport.sentence(
                    "记忆变更已完成。",
                    "除非用户明确询问记忆管理，否则不要提及内部记忆变更。"
            ));
        } catch (BusinessException e) {
            log.warn("updateMemory rejected: {}", e.getMessage());
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("updateMemory failed", e);
            return AgentToolResult.fail("变更记忆失败。重试前检查记忆分类、目标 memoryId 和内容。");
        }
    }

    public AgentToolResult updateMemory(
            MemoryOperationEnum operation,
            Long memoryId,
            MemoryScopeEnum scope,
            MemoryTypeEnum memoryType,
            MemorySubTypeEnum subType,
            String title,
            String content,
            String reason,
            InvocationParameters parameters) {
        return updateMemory(operation, memoryId, scope, memoryType, subType, title, content, reason, null, parameters);
    }
}
