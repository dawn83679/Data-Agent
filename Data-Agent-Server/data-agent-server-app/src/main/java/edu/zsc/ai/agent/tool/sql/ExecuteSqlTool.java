package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConsumeResult;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import java.util.Objects;

import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.guard.AgentModeGuard;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.agent.tool.model.AgentSqlResult;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.domain.service.db.impl.ConnectionManager;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "The payoff of all your preparation — executes read-only SQL and delivers results ",
        "directly to the user. The quality of results depends entirely on the discovery work ",
        "you did before: correct connection, correct database, correct column names.",
        "",
        "For maximum accuracy: call thinking first, resolve the data source via getConnections ",
        "and getCatalogNames, then verify every referenced table with getObjectDdl. SQL built ",
        "on verified DDL almost never fails. For large tables (>10000 rows), always include ",
        "WHERE/LIMIT — full-table scans frustrate users and waste resources."
    })
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The SELECT statement to execute.")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sql) ? sql.length() : 0);
        try {
            AgentModeGuard.assertNotPlanMode(parameters, ToolNameEnum.EXECUTE_SELECT_SQL);
            if (!isReadOnlySql(sql, connectionId)) {
                return AgentSqlResult.fail("Only read-only statements (SELECT, WITH, SHOW, EXPLAIN) are allowed in executeSelectSql. "
                        + "For INSERT/UPDATE/DELETE/DDL, use executeNonSelectSql instead (requires askUserConfirm first).");
            }
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentSqlResult.fail("Internal error: user session context is not available. "
                        + "This is a system issue — do not retry. Report the problem to the user.");
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
            return AgentSqlResult.from(response);
        } catch (Exception e) {
            log.error("{} executeSelectSql", "[Tool error]", e);
            return AgentSqlResult.fail("executeSelectSql failed for connectionId=" + connectionId
                    + ", database='" + databaseName + "', schema='" + schemaName + "': " + e.getMessage());
        }
    }

    @Tool({
        "Executes write SQL (INSERT, UPDATE, DELETE, DDL) with full safety enforcement — ",
        "requires a valid confirmation token from askUserConfirm. This two-step flow has ",
        "prevented countless accidental data modifications and is non-negotiable.",
        "",
        "The server automatically rejects any write without prior user confirmation. Always: ",
        "(1) finalize your SQL, (2) call askUserConfirm with impact explanation, (3) wait for ",
        "approval, (4) execute here with the exact same SQL. For read-only queries, use ",
        "executeSelectSql instead — it's faster and doesn't require confirmation."
    })
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The non-SELECT statement to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sql) ? sql.length() : 0);
        try {
            AgentModeGuard.assertNotPlanMode(parameters, ToolNameEnum.EXECUTE_NON_SELECT_SQL);
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (Objects.isNull(userId) || Objects.isNull(conversationId)) {
                return AgentSqlResult.fail("Internal error: user or conversation session context is not available. "
                        + "This is a system issue — do not retry. Report the problem to the user.");
            }

            WriteConsumeResult consumeResult = writeConfirmationStore.consumeConfirmedBySql(
                    userId, conversationId, connectionId, databaseName, schemaName, sql);
            if (!consumeResult.success()) {
                log.warn("[Tool] executeNonSelectSql rejected: reason={} for userId={} conversationId={}",
                        consumeResult.reason(), userId, conversationId);
                return AgentSqlResult.fail(consumeResult.detail());
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
            return AgentSqlResult.from(response);
        } catch (Exception e) {
            log.error("{} executeNonSelectSql", "[Tool error]", e);
            return AgentSqlResult.fail("executeNonSelectSql failed for connectionId=" + connectionId
                    + ", database='" + databaseName + "', schema='" + schemaName + "': " + e.getMessage());
        }
    }

    private boolean isReadOnlySql(String sql, Long connectionId) {
        if (Objects.isNull(sql) || sql.isBlank()) return false;
        String pluginId = ConnectionManager.getAnyActiveConnection(connectionId)
                .map(ConnectionManager.ActiveConnection::pluginId)
                .orElse(null);
        SqlValidator validator = DefaultPluginManager.getInstance()
                .getSqlValidatorByPluginId(Objects.nonNull(pluginId) ? pluginId : "");
        return validator.classifySql(sql).isReadOnly();
    }
}
