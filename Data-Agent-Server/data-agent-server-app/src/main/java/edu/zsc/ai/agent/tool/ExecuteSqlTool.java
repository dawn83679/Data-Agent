package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.common.constant.RequestContextConstant;
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
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "[WHAT] Execute a SELECT SQL statement on the current connection and database.",
        "[WHEN] Use for all read-only queries. Pass connectionId, databaseName, schemaName from current session context.",
        "IMPORTANT — ALWAYS call countTableRows before executing. If the row count exceeds 10000, MUST add a WHERE clause or LIMIT to avoid fetching excessive data."
    })
    public ExecuteSqlResponse executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The SELECT statement to execute") String sql,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            if (!isReadOnlySql(sql)) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("Only read-only statements (SELECT, WITH, SHOW, EXPLAIN) are allowed.")
                        .build();
            }
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("User context is missing.")
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
            log.info("{} executeSelectSql", "[Tool done]");
            return response;
        } catch (Exception e) {
            log.error("{} executeSelectSql", "[Tool error]", e);
            return ExecuteSqlResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Tool({
        "[WHAT] Execute a write SQL statement (INSERT, UPDATE, DELETE, DDL) on the current connection and database.",
        "[WHEN] Use ONLY after askUserConfirm has been called and the user has confirmed the operation.",
        "IMPORTANT — NEVER call this tool without first calling askUserConfirm. "
            + "The server automatically validates that the user has confirmed this exact SQL. "
            + "If confirmation is missing or expired, the call will be rejected."
    })
    public ExecuteSqlResponse executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The non-SELECT statement to execute (INSERT, UPDATE, DELETE, DDL, etc.)") String sql,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (userId == null || conversationId == null) {
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("User or conversation context is missing.")
                        .build();
            }

            // Server-side gate: find and consume a CONFIRMED token for this user + conversation + sql.
            // The agent does not need to know or pass the token.
            boolean consumed = writeConfirmationStore.consumeConfirmedBySql(userId, conversationId, sql);
            if (!consumed) {
                log.warn("[Tool] executeNonSelectSql rejected: no CONFIRMED token for userId={} conversationId={}",
                        userId, conversationId);
                return ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("Write operation rejected: no valid user confirmation found. "
                                + "You must call askUserConfirm first and wait for the user to confirm.")
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
            log.info("{} executeNonSelectSql", "[Tool done]");
            return response;
        } catch (Exception e) {
            log.error("{} executeNonSelectSql", "[Tool error]", e);
            return ExecuteSqlResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private boolean isReadOnlySql(String sql) {
        if (sql == null || sql.isBlank()) return false;
        String stripped = sql.stripLeading().replaceAll("(?s)/\\*.*?\\*/", "").stripLeading();
        String firstWord = stripped.split("\\s+")[0].toUpperCase();
        return switch (firstWord) {
            case "SELECT", "WITH", "SHOW", "EXPLAIN" -> true;
            default -> false;
        };
    }
}
