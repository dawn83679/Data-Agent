package edu.zsc.ai.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FunctionTool {

    private final FunctionService functionService;

    @Tool({
        "Get the list of all function names and metadata in the current database/schema.",
        "Use when the user asks what functions exist or to explore database functions. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getFunctionNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getFunctionNames, connectionId={}, database={}, schema={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE,
                connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<FunctionMetadata> functions = functionService.listFunctions(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (functions == null || functions.isEmpty()) {
                log.info("{} getFunctionNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_FUNCTIONS);
                return ToolMessageConstants.EMPTY_NO_FUNCTIONS;
            }

            log.info("{} getFunctionNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, functions.size());
            return functions.toString();
        } catch (Exception e) {
            log.error("{} getFunctionNames", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "Get the DDL (Data Definition Language) statement for a specific function.",
        "Use when the user needs the function definition or CREATE FUNCTION statement. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getFunctionDdl(
            @P("The exact name of the function in the current schema") String functionName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getFunctionDdl, functionName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, functionName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            String ddl = functionService.getFunctionDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    functionName,
                    userId
            );

            log.info("{} getFunctionDdl, functionName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    functionName, ddl != null ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getFunctionDdl, functionName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, functionName, e);
            return e.getMessage();
        }
    }
}
