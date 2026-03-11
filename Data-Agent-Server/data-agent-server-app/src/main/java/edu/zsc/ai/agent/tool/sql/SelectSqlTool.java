package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ToolContext;
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
public class SelectSqlTool {

    private final SqlExecutionService sqlExecutionService;

    @Tool({
        "Executes read-only SQL (SELECT, WITH, SHOW, EXPLAIN). Accepts a list of statements — ",
        "pass multiple related queries in one call to reduce round-trips.",
        "",
        "Use when: executing SELECT queries after verifying schema.",
        "Skip when: task requires writes (use executeNonSelectSql instead).",
        "",
        "IMPORTANT: For tables >10k rows, always include WHERE/LIMIT."
    })
    public AgentSqlResult executeSelectSql(
            @P("Connection id") Long connectionId,
            @P("Database (catalog) name") String databaseName,
            @P(value = "Schema name; omit if N/A", required = false) String schemaName,
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
