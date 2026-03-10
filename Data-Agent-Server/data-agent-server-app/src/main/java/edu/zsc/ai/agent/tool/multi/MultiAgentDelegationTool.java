package edu.zsc.ai.agent.tool.multi;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.domain.service.agent.multi.model.SubAgentDelegationResult;
import edu.zsc.ai.domain.service.agent.multi.MultiAgentDelegationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@AgentTool
@RequiredArgsConstructor
public class MultiAgentDelegationTool {

    private final MultiAgentDelegationService delegationService;

    @Tool("Delegate schema discovery and object verification to the schema analyst. Use this when the relevant tables, views, or fields are still unclear.")
    public AgentToolResult delegateToSchemaAnalyst(
            @P("Short title for this delegated task") String title,
            @P("Concrete instructions for the schema analyst") String instructions,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            return ctx.timed(toToolResult(
                    delegationService.delegate(AgentRoleEnum.SCHEMA_ANALYST, title, instructions, parameters)));
        }
    }

    @Tool("Delegate SQL design and execution planning to the SQL planner. Use this after schema discovery or when a precise SQL plan is required.")
    public AgentToolResult delegateToSqlPlanner(
            @P("Short title for this delegated task") String title,
            @P("Concrete instructions for the SQL planner") String instructions,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            return ctx.timed(toToolResult(
                    delegationService.delegate(AgentRoleEnum.SQL_PLANNER, title, instructions, parameters)));
        }
    }

    @Tool("Delegate query execution or write approval flow to the SQL executor. Use this only when execution-ready SQL or precise execution steps already exist.")
    public AgentToolResult delegateToSqlExecutor(
            @P("Short title for this delegated task") String title,
            @P("Concrete instructions for the SQL executor") String instructions,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            return ctx.timed(toToolResult(
                    delegationService.delegate(AgentRoleEnum.SQL_EXECUTOR, title, instructions, parameters)));
        }
    }

    @Tool("Delegate final interpretation and user-facing synthesis to the result analyst. Use this when you want a concise final summary of what happened.")
    public AgentToolResult delegateToResultAnalyst(
            @P("Short title for this delegated task") String title,
            @P("Concrete instructions for the result analyst") String instructions,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            return ctx.timed(toToolResult(
                    delegationService.delegate(AgentRoleEnum.RESULT_ANALYST, title, instructions, parameters)));
        }
    }

    private AgentToolResult toToolResult(SubAgentDelegationResult result) {
        if (result == null) {
            return AgentToolResult.fail("Sub-agent delegation returned no result.");
        }
        if ("failed".equalsIgnoreCase(result.getStatus())) {
            return AgentToolResult.fail(StringUtils.defaultIfBlank(
                    result.getSummary(),
                    "Sub-agent task failed."));
        }
        return AgentToolResult.success(result);
    }
}
