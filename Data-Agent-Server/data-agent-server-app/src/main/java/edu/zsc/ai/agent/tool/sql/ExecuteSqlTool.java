package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.agent.tool.model.AgentSqlResult;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "[GOAL] Execute finalized read-only SQL after intent, scope, and filters are clear.",
        "[PRECHECK] Confirm target source is resolved, tableName is user-confirmed, and filter semantics (time range/category values) are validated.",
        "[WHEN] Use for SELECT/WITH/SHOW/EXPLAIN only. Prefer after countObjectRows + necessary schema checks.",
        "[TABLE] tableName must be user-confirmed and fully qualified (schema.table or catalog.schema.table); never guess ambiguous objects.",
        "[CONSISTENCY] tableName parameter and the main table referenced in SQL MUST be identical. Mismatch may crash the system.",
        "[SAFETY] For large tables (>10000 rows), SQL should include WHERE/LIMIT to avoid excessive scans.",
        "[FAILSAFE] If tableName or SQL safety checks fail, stop execution and return clarification/error."
    })
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("Main table name used by this SQL. Must be USER-CONFIRMED, fully qualified, and EXACTLY match the main table referenced in SQL; mismatch may crash the system.")
            String tableName,
            @P("The SELECT statement to execute. Its main table MUST exactly match tableName.")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, tableName={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName, tableName,
                sql != null ? sql.length() : 0);
        try {
            String tableValidationError = validateQualifiedTableName(tableName, true);
            if (tableValidationError != null) {
                return AgentSqlResult.fail(tableValidationError);
            }
            if (!isReadOnlySql(sql)) {
                return AgentSqlResult.fail("Only read-only statements (SELECT, WITH, SHOW, EXPLAIN) are allowed.");
            }
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentSqlResult.fail("User context is missing.");
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
            return AgentSqlResult.fail(e.getMessage());
        }
    }

    @Tool({
        "[GOAL] Execute finalized write SQL only after explicit user approval.",
        "[PRECHECK] SQL must be exact, impact-assessed, and previously confirmed via askUserConfirm.",
        "[WHEN] Use for INSERT/UPDATE/DELETE/DDL only; do not use for read-only queries.",
        "[TABLE] tableName (if provided) must be user-confirmed and fully qualified; never use guessed ambiguous names.",
        "[CONSISTENCY] If tableName is provided, it MUST be identical to the main table referenced in SQL. Mismatch may crash the system.",
        "[SAFETY] Server validates user confirmation against exact SQL and scope. Missing/expired confirmation must be rejected.",
        "[FAILSAFE] On rejection or validation failure, stop execution and return to user clarification/approval flow."
    })
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P(value = "Optional main table name. If provided, it must be USER-CONFIRMED, fully qualified, and EXACTLY match the main table in SQL; mismatch may crash the system.", required = false)
            String tableName,
            @P("The non-SELECT statement to execute (INSERT, UPDATE, DELETE, DDL, etc.). If tableName is provided, SQL main table MUST exactly match it.")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, tableName={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName, tableName,
                sql != null ? sql.length() : 0);
        try {
            String tableValidationError = validateQualifiedTableName(tableName, false);
            if (tableValidationError != null) {
                return AgentSqlResult.fail(tableValidationError);
            }
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (userId == null || conversationId == null) {
                return AgentSqlResult.fail("User or conversation context is missing.");
            }

            // Server-side gate: find and consume a CONFIRMED token for this user + conversation + connection + database + schema + sql.
            // The agent does not need to know or pass the token.
            boolean consumed = writeConfirmationStore.consumeConfirmedBySql(
                    userId, conversationId, connectionId, databaseName, schemaName, sql);
            if (!consumed) {
                log.warn("[Tool] executeNonSelectSql rejected: no CONFIRMED token for userId={} conversationId={}",
                        userId, conversationId);
                return AgentSqlResult.fail("Write operation rejected: no valid user confirmation found. "
                        + "You must call askUserConfirm first and wait for the user to confirm.");
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
            return AgentSqlResult.fail(e.getMessage());
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

    private String validateQualifiedTableName(String tableName, boolean required) {
        if (StringUtils.isBlank(tableName)) {
            return required
                    ? "tableName is required and must be fully qualified (schema.table or catalog.schema.table)."
                    : null;
        }

        String normalized = tableName.trim();
        if (!normalized.contains(".") || normalized.startsWith(".") || normalized.endsWith(".") || normalized.contains("..")) {
            return "tableName must be fully qualified and contain a valid dot path (schema.table or catalog.schema.table).";
        }
        if (normalized.contains(" ")) {
            return "tableName must not contain spaces.";
        }
        return null;
    }
}
