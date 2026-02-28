package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;

    @Tool({
        "Execute a SELECT SQL statement on the current connection and database.",
        "Use this tool to query data. Pass connectionId, databaseName, schemaName from current session context and the SELECT SQL to run."
    })
    public ExecuteSqlResponse executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The SELECT statement to execute") String sql,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage(ToolMessageConstants.USER_CONTEXT_MISSING)
                        .build();
            }
            AgentExecuteSqlRequest request = AgentExecuteSqlRequest.builder()
                    .connectionId(connectionId)
                    .databaseName(databaseName)
                    .schemaName(schemaName)
                    .sql(sql)
                    .userId(userId)
                    .build();
            ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
            log.info("{} executeSelectSql", ToolMessageConstants.TOOL_LOG_PREFIX_DONE);
            return response;
        } catch (Exception e) {
            log.error("{} executeSelectSql", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return ExecuteSqlResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Tool({
        "Execute a non-SELECT SQL statement (INSERT, UPDATE, DELETE, DDL, etc.) on the current connection and database.",
        "Use this tool to modify data or schema. Pass connectionId, databaseName, schemaName from current session context and the SQL to run."
    })
    public ExecuteSqlResponse executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The non-SELECT statement to execute (INSERT, UPDATE, DELETE, DDL, etc.)") String sql,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage(ToolMessageConstants.USER_CONTEXT_MISSING)
                        .build();
            }
            AgentExecuteSqlRequest request = AgentExecuteSqlRequest.builder()
                    .connectionId(connectionId)
                    .databaseName(databaseName)
                    .schemaName(schemaName)
                    .sql(sql)
                    .userId(userId)
                    .build();
            ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
            log.info("{} executeNonSelectSql", ToolMessageConstants.TOOL_LOG_PREFIX_DONE);
            return response;
        } catch (Exception e) {
            log.error("{} executeNonSelectSql", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return ExecuteSqlResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
