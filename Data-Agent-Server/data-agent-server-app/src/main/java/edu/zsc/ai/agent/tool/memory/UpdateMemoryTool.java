package edu.zsc.ai.agent.tool.memory;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
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
            "Use this tool only for durable memory that should affect future turns.",
            "CREATE stores a new durable memory. UPDATE revises an existing one. DELETE disables an outdated or wrong one.",
            "Write memory only for stable preferences, stable rules, verified facts, field definitions, object knowledge, or reusable SQL patterns.",
            "Do not write memory for one-off requests, temporary context, guesses, or raw chat wording.",
            "For UPDATE or DELETE, usually call readMemory first and use the returned memoryId.",
            "content must be short, factual, reusable, and specific enough to apply later.",
            "If the memory is about a concrete table, view, schema, or connection, include the exact identifiers you know.",
            "Choose the narrowest valid scope. USER is for cross-conversation memory. CONVERSATION is only for memory that should stay inside this conversation.",
            "Do not narrate the tool call to the user. Just continue the task with the updated memory state."
    })
    public AgentToolResult updateMemory(
            @P("Operation: CREATE, UPDATE, or DELETE") MemoryOperationEnum operation,
            @P(value = "Target memoryId. Required for UPDATE and DELETE. Omit for CREATE.", required = false) Long memoryId,
            @P(value = "Memory scope: USER or CONVERSATION. Required for CREATE, optional for UPDATE, ignored for DELETE.", required = false) MemoryScopeEnum scope,
            @P(value = "Memory type: PREFERENCE, BUSINESS_RULE, KNOWLEDGE_POINT, WORKFLOW_CONSTRAINT, or GOLDEN_SQL_CASE. Required for CREATE, optional for UPDATE, ignored for DELETE.", required = false) MemoryTypeEnum memoryType,
            @P(value = "Memory subType. Must exactly match one of: RESPONSE_FORMAT, LANGUAGE_PREFERENCE, PRODUCT_RULE, DOMAIN_RULE, GOVERNANCE_RULE, SAFETY_RULE, ARCHITECTURE_KNOWLEDGE, DOMAIN_KNOWLEDGE, GLOSSARY, OBJECT_KNOWLEDGE, PROCESS_RULE, APPROVAL_RULE, IMPLEMENTATION_CONSTRAINT, REVIEW_CONSTRAINT, QUERY_PATTERN, JOIN_STRATEGY, VALIDATED_SQL, METRIC_CALCULATION. Required for CREATE, optional for UPDATE, ignored for DELETE.", required = false) MemorySubTypeEnum subType,
            @P(value = "Short memory title. Optional for CREATE or UPDATE, ignored for DELETE.", required = false) String title,
            @P(value = "Authoritative durable memory content. Required for CREATE and UPDATE. For object-level scope or concrete database knowledge, include exact identifiers when known: connectionId, connection name, catalog/database, schema, and object/table/view name.", required = false) String content,
            @P(value = "Short reason explaining why this memory should persist, be revised, or be removed", required = false) String reason,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (agentType != AgentTypeEnum.MAIN || agentMode != AgentModeEnum.AGENT) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "updateMemory is only available to the main agent in normal execution mode.",
                    "Do not attempt to mutate memory from this agent context."
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
                    "Memory mutation completed.",
                    "Do not mention the internal memory mutation unless the user explicitly asks about memory management."
            ));
        } catch (BusinessException e) {
            log.warn("updateMemory rejected: {}", e.getMessage());
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("updateMemory failed", e);
            return AgentToolResult.fail("Failed to mutate memory. Review the memory classification, target memoryId, and content before retrying.");
        }
    }
}
