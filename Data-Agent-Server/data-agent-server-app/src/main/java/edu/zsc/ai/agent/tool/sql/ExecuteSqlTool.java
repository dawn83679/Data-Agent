package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConsumeResult;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.SqlToolMessageSupport;
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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "Value: runs read-only SQL and returns real database results. This is the reliable basis for any user-facing query answer.",
        "Use When: call after the target connection, database, schema, and referenced objects have been verified.",
        "Preconditions: sqls is required and every statement must be read-only. For large tables, include WHERE or LIMIT before executing.",
        "After Success: base the answer strictly on the returned results. If the user needs a visualization, pass the verified result set into renderChart.",
        "After Failure: inspect the statement-level errors, fix the SQL or scope, and retry only when the query is valid. Do not claim the query succeeded or fabricate results.",
        "Do Not Use When: the statement writes data or the referenced objects are still unverified.",
        "Relation: typically after getEnvironmentOverview, searchObjects, and getObjectDetail, or after callingPlannerSubAgent for a read plan. Results are returned in the results array."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL)
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of read-only SQL statements to execute.")
            List<String> sqls,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sqls) ? sqls.size() : 0);
        if (Objects.isNull(sqls) || sqls.isEmpty()) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_SELECT_SQL,
                    SqlToolMessageSupport.requireReadStatements(connectionId, databaseName, schemaName)
            );
        }
        if (!allReadOnly(sqls, connectionId)) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_SELECT_SQL,
                    SqlToolMessageSupport.requireReadOnlyStatements(connectionId, databaseName, schemaName)
            );
        }
        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, false);
        log.info("{} executeSelectSql", "[Tool done]");
        return AgentSqlResult.fromBatch(responses);
    }

    @Tool({
        "Value: executes approved write SQL and turns user approval into the actual database change.",
        "Use When: call only after the SQL is finalized and askUserConfirm approved the exact same SQL for the same database context.",
        "Preconditions: sqls is required. The confirmation must already exist for the exact SQL text and current connection, database, and schema.",
        "After Success: report only the actual write outcome from the returned results. If the user needs verification, follow with executeSelectSql.",
        "After Failure: inspect the statement-level errors, explain that the write did not complete as intended, and do not assume database state beyond the returned results.",
        "Do Not Use When: confirmation is missing, the SQL changed after confirmation, or the statements are read-only queries.",
        "Relation: finalize SQL, call askUserConfirm, then call executeNonSelectSql with the exact same SQL. Results are returned in the results array."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_NON_SELECT_SQL)
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of non-SELECT SQL statements to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            List<String> sqls,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sqls) ? sqls.size() : 0);
        if (Objects.isNull(sqls) || sqls.isEmpty()) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_NON_SELECT_SQL,
                    SqlToolMessageSupport.requireWriteStatements(connectionId, databaseName, schemaName)
            );
        }

        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        String joinedSql = String.join(";\n", sqls);
        WriteConsumeResult consumeResult = writeConfirmationStore.consumeConfirmedBySql(db, joinedSql);
        if (!consumeResult.success()) {
            log.warn("[Tool] executeNonSelectSql rejected: reason={}", consumeResult.reason());
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_NON_SELECT_SQL,
                    SqlToolMessageSupport.confirmationBlocked(connectionId, databaseName, schemaName, consumeResult.detail())
            );
        }

        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, true);
        log.info("{} executeNonSelectSql", "[Tool done]");
        return AgentSqlResult.fromBatch(responses);
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

    private void annotateSqlFailures(List<ExecuteSqlResponse> responses,
                                     Long connectionId,
                                     String databaseName,
                                     String schemaName,
                                     List<String> sqls,
                                     boolean writeOperation) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        for (int i = 0; i < responses.size(); i++) {
            ExecuteSqlResponse response = responses.get(i);
            if (response == null || response.isSuccess()) {
                continue;
            }
            String sqlPreview = buildSqlPreview(sqls, i);
            String currentError = StringUtils.defaultIfBlank(response.getErrorMessage(), "unknown database error");
            response.setErrorMessage(SqlToolMessageSupport.failureMessage(
                    writeOperation,
                    connectionId,
                    databaseName,
                    schemaName,
                    i,
                    responses.size(),
                    sqlPreview,
                    currentError
            ));
        }
    }

    private String buildSqlPreview(List<String> sqls, int index) {
        if (sqls == null || index < 0 || index >= sqls.size()) {
            return "";
        }
        String sql = StringUtils.normalizeSpace(sqls.get(index));
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        return sql.length() > 80 ? sql.substring(0, 77) + "..." : sql;
    }
}
