package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.model.dto.request.db.ExecuteSqlRequest;
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
        "Execute a single SQL statement (SELECT, INSERT, UPDATE, DELETE, etc.) on the current connection and database.",
        "Use after generating SQL from natural language. Pass connectionId, databaseName, schemaName from current session context and the SQL to run."
    })
    public ExecuteSqlResponse executeSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The SQL statement to execute") String sql,
            InvocationParameters parameters) {
        log.info("[Tool before] executeSql, connectionId={}, database={}, schema={}, sqlLength={}",
                connectionId, databaseName, schemaName, sql != null ? sql.length() : 0);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("User context missing.")
                        .build();
            }
            ExecuteSqlRequest request = ExecuteSqlRequest.builder()
                    .connectionId(connectionId)
                    .databaseName(databaseName)
                    .schemaName(schemaName)
                    .sql(sql)
                    .userId(userId)
                    .build();
            ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
            log.info("[Tool done] executeSql");
            return response;
        } catch (Exception e) {
            log.error("[Tool error] executeSql", e);
            return ExecuteSqlResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
