package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.sql.approval.WriteExecutionApprovalStore;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.SqlToolMessageSupport;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.agent.tool.sql.model.ExecuteNonSelectToolResult;
import edu.zsc.ai.agent.tool.sql.model.WriteExecutionConfirmationPayload;
import edu.zsc.ai.agent.tool.sql.model.WriteExecutionGrantOption;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import edu.zsc.ai.domain.service.permission.PermissionRuleService;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.domain.service.db.impl.ActiveConnectionRegistry;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final PermissionRuleService permissionRuleService;
    private final WriteExecutionApprovalStore writeExecutionApprovalStore;
    private final ConnectionAccessService connectionAccessService;

    @Tool({
        "Value: executes read-only SQL and returns real database results.",
        "Use When: scope, objects, and SQL are verified and the user needs actual query evidence.",
        "Preconditions: sqls is required and every statement must be read-only; use WHERE or LIMIT for large tables.",
        "Result: database result sets; use them as the only source of truth for the answer.",
        "Boundary: never use for writes or unverified object structure."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL)
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of read-only SQL statements to execute.")
            List<String> sqls,
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
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
        connectionAccessService.assertReadable(connectionId);
        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, false);
        log.info("{} executeSelectSql", "[Tool done]");
        return AgentSqlResult.fromBatch(responses);
    }

    public AgentSqlResult executeSelectSql(
            Long connectionId,
            String databaseName,
            String schemaName,
            List<String> sqls,
            InvocationParameters parameters) {
        return executeSelectSql(connectionId, databaseName, schemaName, sqls, null, parameters);
    }

    @Tool({
        "Value: executes write SQL when permission already allows it, otherwise returns a structured confirmation payload for the frontend.",
        "Use When: write SQL is finalized and ready for execution or explicit confirmation.",
        "Preconditions: sqls is required and must contain write statements only.",
        "Result: EXECUTED means the write ran; REQUIRES_CONFIRMATION means nothing ran yet and the frontend must ask the user.",
        "Boundary: never use for read-only queries or guessed write SQL."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_NON_SELECT_SQL)
    public ExecuteNonSelectToolResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of non-SELECT SQL statements to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            List<String> sqls,
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
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

        connectionAccessService.assertReadable(connectionId);
        connectionAccessService.assertWritableForCurrentWorkspace(connectionId);

        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        String joinedSql = String.join(";\n", sqls);
        boolean ruleMatched = permissionRuleService.matchesEnabledRule(connectionId, databaseName, schemaName);
        boolean approvalMatched = !ruleMatched && writeExecutionApprovalStore.consumeApproved(db, joinedSql);

        if (!ruleMatched && !approvalMatched) {
            log.info("[Tool] executeNonSelectSql requires confirmation for connectionId={}, database={}, schema={}",
                    connectionId, databaseName, schemaName);
            return ExecuteNonSelectToolResult.requiresConfirmation(
                    WriteExecutionConfirmationPayload.builder()
                            .conversationId(RequestContext.getConversationId())
                            .connectionId(connectionId)
                            .databaseName(databaseName)
                            .schemaName(schemaName)
                            .sql(joinedSql)
                            .sqlPreview(joinedSql)
                            .availableGrantOptions(buildAvailableGrantOptions(databaseName, schemaName))
                            .build(),
                    SqlToolMessageSupport.confirmationRequired(connectionId, databaseName, schemaName)
            );
        }

        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, true);
        log.info("{} executeNonSelectSql", "[Tool done]");
        return ExecuteNonSelectToolResult.executed(
                ruleMatched,
                AgentSqlResult.fromBatch(responses),
                ruleMatched
                        ? "Write SQL executed using a default-allow permission."
                        : "Write SQL executed after explicit user approval."
        );
    }

    public ExecuteNonSelectToolResult executeNonSelectSql(
            Long connectionId,
            String databaseName,
            String schemaName,
            List<String> sqls,
            InvocationParameters parameters) {
        return executeNonSelectSql(connectionId, databaseName, schemaName, sqls, null, parameters);
    }

    private List<WriteExecutionGrantOption> buildAvailableGrantOptions(String databaseName, String schemaName) {
        List<WriteExecutionGrantOption> options = new ArrayList<>();
        boolean hasDatabase = StringUtils.isNotBlank(databaseName);
        boolean hasSchema = hasDatabase && StringUtils.isNotBlank(schemaName);

        if (hasSchema) {
            options.add(grantOption(PermissionScopeType.CONVERSATION, PermissionGrantPreset.EXACT_SCHEMA));
        }
        if (hasDatabase) {
            options.add(grantOption(PermissionScopeType.CONVERSATION, PermissionGrantPreset.DATABASE_ALL_SCHEMAS));
        }
        options.add(grantOption(PermissionScopeType.CONVERSATION, PermissionGrantPreset.CONNECTION_ALL_DATABASES));

        if (hasSchema) {
            options.add(grantOption(PermissionScopeType.USER, PermissionGrantPreset.EXACT_SCHEMA));
        }
        if (hasDatabase) {
            options.add(grantOption(PermissionScopeType.USER, PermissionGrantPreset.DATABASE_ALL_SCHEMAS));
        }
        options.add(grantOption(PermissionScopeType.USER, PermissionGrantPreset.CONNECTION_ALL_DATABASES));
        return options;
    }

    private WriteExecutionGrantOption grantOption(PermissionScopeType scopeType, PermissionGrantPreset grantPreset) {
        return WriteExecutionGrantOption.builder()
                .scopeType(scopeType)
                .grantPreset(grantPreset)
                .build();
    }

    private boolean allReadOnly(List<String> sqls, Long connectionId) {
        if (Objects.isNull(sqls) || sqls.isEmpty()) return false;
        String pluginId = ActiveConnectionRegistry.getAnyActiveConnection(connectionId)
                .map(ActiveConnectionRegistry.ActiveConnection::pluginId)
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
            String sqlPreview = "";
            if (sqls != null && i >= 0 && i < sqls.size()) {
                sqlPreview = StringUtils.defaultString(StringUtils.normalizeSpace(sqls.get(i)));
            }
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
}
