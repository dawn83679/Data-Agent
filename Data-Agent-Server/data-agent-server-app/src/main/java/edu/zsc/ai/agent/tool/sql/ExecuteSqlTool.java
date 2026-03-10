package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConsumeResult;
import edu.zsc.ai.agent.guard.AgentModeGuard;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.domain.service.db.impl.ConnectionManager;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;


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
        "Accepts a list of SQL statements. Pass multiple related queries in one call to reduce ",
        "round-trips — results are returned in a 'results' array, one entry per statement.",
        "",
        "For maximum accuracy: call thinking first, resolve the data source via getEnvironmentOverview ",
        "or searchObjects, then verify every referenced table with getObjectDetail. SQL built ",
        "on verified DDL almost never fails. For large tables (>10000 rows), always include ",
        "WHERE/LIMIT — full-table scans frustrate users and waste resources."
    })
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of read-only SQL statements to execute.")
            List<String> sqls,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                    "[Tool]", connectionId, databaseName, schemaName,
                    Objects.nonNull(sqls) ? sqls.size() : 0);
            AgentModeGuard.assertNotPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL);
            if (!allReadOnly(sqls, connectionId)) {
                return AgentSqlResult.fail("Only read-only statements (SELECT, WITH, SHOW, EXPLAIN) are allowed in executeSelectSql. "
                        + "For INSERT/UPDATE/DELETE/DDL, use executeNonSelectSql instead (requires askUserConfirm first).");
            }
            DbContext db = new DbContext(connectionId, databaseName, schemaName);
            List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
            log.info("{} executeSelectSql", "[Tool done]");
            return AgentSqlResult.fromBatch(responses);
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
        "Accepts a list of SQL statements. Pass multiple related statements in one call — ",
        "results are returned in a 'results' array, one entry per statement.",
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
            @P("List of non-SELECT SQL statements to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            List<String> sqls,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                    "[Tool]", connectionId, databaseName, schemaName,
                    Objects.nonNull(sqls) ? sqls.size() : 0);
            AgentModeGuard.assertNotPlanMode(ToolNameEnum.EXECUTE_NON_SELECT_SQL);

            DbContext db = new DbContext(connectionId, databaseName, schemaName);
            String joinedSql = String.join(";\n", sqls);
            WriteConsumeResult consumeResult = writeConfirmationStore.consumeConfirmedBySql(db, joinedSql);
            if (!consumeResult.success()) {
                log.warn("[Tool] executeNonSelectSql rejected: reason={}", consumeResult.reason());
                return AgentSqlResult.fail(consumeResult.detail());
            }

            List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
            log.info("{} executeNonSelectSql", "[Tool done]");
            return AgentSqlResult.fromBatch(responses);
        } catch (Exception e) {
            log.error("{} executeNonSelectSql", "[Tool error]", e);
            return AgentSqlResult.fail("executeNonSelectSql failed for connectionId=" + connectionId
                    + ", database='" + databaseName + "', schema='" + schemaName + "': " + e.getMessage());
        }
    }

    private boolean allReadOnly(List<String> sqls, Long connectionId) {
        if (Objects.isNull(sqls) || sqls.isEmpty()) return false;
        String pluginId = ConnectionManager.getAnyActiveConnection(connectionId)
                .map(ConnectionManager.ActiveConnection::pluginId)
                .orElse(null);
        SqlValidator validator = DefaultPluginManager.getInstance()
                .getSqlValidatorByPluginId(Objects.nonNull(pluginId) ? pluginId : "");
        return sqls.stream().allMatch(stmt -> validator.classifySql(stmt).isReadOnly());
    }
}
