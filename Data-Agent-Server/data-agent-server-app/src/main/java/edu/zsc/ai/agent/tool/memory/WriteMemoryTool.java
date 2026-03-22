package edu.zsc.ai.agent.tool.memory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.MemoryRecallConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryToolActionEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.ai.MemoryWriteRequest;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class WriteMemoryTool {

    private final MemoryService memoryService;

    @Tool({
            "Value: writes durable structured memory for future prompt injection and workspace continuity.",
            "Use When: call only when the conversation reveals stable preferences, durable constraints, business rules, validated knowledge, or reusable SQL patterns that should help future turns.",
            "Durability Test: write memory only if the information should still be useful beyond this turn. Stable preferences, verified rules, and reusable patterns qualify; one-off requests, emotions, and speculative claims do not.",
            "Preconditions: choose the narrowest valid scope, memoryType, and subType; content must be concise, durable, and reusable rather than conversational.",
            "Scope Guidance: use USER for durable user preferences or habits, WORKSPACE for database or project rules tied to the current environment, and CONVERSATION only for short-lived but reusable constraints inside this conversation.",
            "Workspace Binding: when scope=WORKSPACE, you must explicitly choose workspaceLevel and provide the matching binding fields based on the current task context.",
            "Classification Guidance: PREFERENCE is for stable response or interaction preferences; BUSINESS_RULE and WORKFLOW_CONSTRAINT are for durable rules; KNOWLEDGE_POINT is for verified facts; GOLDEN_SQL_CASE is for validated reusable SQL patterns.",
            "Optional: activateSkill(\"memory\") only if you need the extended memory classification guide; the tool itself does not require that skill to run.",
            "After Success: continue the task normally; do not narrate the memory write as the user-facing result.",
            "After Failure: fix the classification or content and retry only if the information is truly durable.",
            "Do Not Use When: the information is temporary, emotional-only, speculative, or specific to a one-off task."
    })
    public AgentToolResult writeMemory(
            @P("Memory scope: USER/WORKSPACE/CONVERSATION") String scope,
            @P(value = "Workspace binding level when scope=WORKSPACE: GLOBAL/CONNECTION/CATALOG/SCHEMA", required = false) String workspaceLevel,
            @P(value = "Explicit connectionId when workspaceLevel is CONNECTION/CATALOG/SCHEMA", required = false) Long workspaceConnectionId,
            @P(value = "Explicit catalogName when workspaceLevel is CATALOG/SCHEMA", required = false) String workspaceCatalogName,
            @P(value = "Explicit schemaName when workspaceLevel is SCHEMA", required = false) String workspaceSchemaName,
            @P("Memory type: PREFERENCE/BUSINESS_RULE/KNOWLEDGE_POINT/WORKFLOW_CONSTRAINT/GOLDEN_SQL_CASE") String memoryType,
            @P("Memory subType matching the chosen memoryType") String subType,
            @P("Short memory title") String title,
            @P("Authoritative durable memory content") String content,
            @P(value = "Short reason explaining why this memory should persist", required = false) String reason,
            @P(value = "Optional confidence score between 0 and 1", required = false) Double confidence,
            @P(value = "Optional source message ids that justify this memory", required = false) List<String> sourceMessageIds,
            InvocationParameters parameters) {

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (agentType != AgentTypeEnum.MAIN || agentMode != AgentModeEnum.AGENT) {
            return AgentToolResult.fail(ToolMessageSupport.sentence(
                    "writeMemory is only available to the main agent in normal execution mode.",
                    "Do not attempt to write memory from this agent context."
            ));
        }

        try {
            MemoryWriteResult writeResult = memoryService.writeAgentMemory(MemoryWriteRequest.builder()
                    .scope(scope)
                    .workspaceLevel(workspaceLevel)
                    .workspaceConnectionId(workspaceConnectionId)
                    .workspaceCatalogName(workspaceCatalogName)
                    .workspaceSchemaName(workspaceSchemaName)
                    .memoryType(memoryType)
                    .subType(subType)
                    .title(title)
                    .content(content)
                    .reason(reason)
                    .confidence(confidence)
                    .sourceMessageIds(sourceMessageIds)
                    .build());
            AiMemory memory = writeResult.getMemory();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put(MemoryRecallConstant.ITEM_MEMORY_ID, memory.getId());
            result.put(MemoryRecallConstant.ITEM_SCOPE, memory.getScope());
            result.put(MemoryRecallConstant.ITEM_WORKSPACE_LEVEL, memory.getWorkspaceLevel());
            result.put(MemoryRecallConstant.ITEM_WORKSPACE_CONTEXT_KEY, memory.getWorkspaceContextKey());
            result.put(MemoryRecallConstant.ITEM_MEMORY_TYPE, memory.getMemoryType());
            result.put(MemoryRecallConstant.ITEM_SUB_TYPE, memory.getSubType());
            String action = writeResult.getAction() == null
                    ? MemoryToolActionEnum.UNKNOWN.getCode()
                    : writeResult.getAction().getCode();
            result.put(MemoryRecallConstant.ITEM_ACTION, action);
            log.info("[Tool done] writeMemory, action={}, memoryId={}, scope={}, workspaceLevel={}, memoryType={}, subType={}",
                    action,
                    memory.getId(),
                    memory.getScope(),
                    memory.getWorkspaceLevel(),
                    memory.getMemoryType(),
                    memory.getSubType());
            return AgentToolResult.success(result, ToolMessageSupport.sentence(
                    "Memory write completed.",
                    "Do not mention the internal memory write unless the user explicitly asks about memory management."
            ));
        } catch (BusinessException e) {
            log.warn("writeMemory rejected: {}", e.getMessage());
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("writeMemory failed", e);
            return AgentToolResult.fail("Failed to write memory. Review the memory classification and content before retrying.");
        }
    }
}
