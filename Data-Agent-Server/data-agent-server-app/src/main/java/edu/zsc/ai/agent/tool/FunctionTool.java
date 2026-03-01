package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class FunctionTool {

    private final FunctionService functionService;

    @Tool({
        "[WHAT] List all user-defined functions in the current database/schema.",
        "[WHEN] Use when the user asks about functions or wants to use one in a query. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getFunctionNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getFunctionNames, connectionId={}, database={}, schema={}", connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<FunctionMetadata> functions = functionService.getFunctions(connectionId, databaseName, schemaName, userId);
            if (functions == null || functions.isEmpty()) {
                log.info("[Tool done] getFunctionNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getFunctionNames, result size={}", functions.size());
            return AgentToolResult.success(functions);
        } catch (Exception e) {
            log.error("[Tool error] getFunctionNames", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[WHAT] Get the CREATE FUNCTION DDL for a specific user-defined function.",
        "[WHEN] Use when you need to understand a function's logic, parameters, or return type. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getFunctionDdl(
            @P("The exact name of the function in the current schema") String functionName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getFunctionDdl, functionName={}, connectionId={}, database={}, schema={}", functionName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            String ddl = functionService.getFunctionDdl(connectionId, databaseName, schemaName, functionName, userId);
            log.info("[Tool done] getFunctionDdl, functionName={}, ddlLength={}", functionName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return AgentToolResult.success(ddl);
        } catch (Exception e) {
            log.error("[Tool error] getFunctionDdl, functionName={}", functionName, e);
            return AgentToolResult.fail(e);
        }
    }
}
