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
        "价值：执行只读 SQL 并返回真实数据库结果。",
        "使用时机：范围、对象和 SQL 已验证，且用户需要真实查询证据。",
        "前置条件：sqls 必填，且每条语句必须只读；大表查询需要 WHERE 或 LIMIT。",
        "结果：数据库结果集；回答只能以这些结果为事实来源。",
        "边界：不要用于写入操作，也不要针对未验证对象结构执行。"
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL)
    public AgentSqlResult executeSelectSql(
            @P("当前会话范围内的连接 ID") Long connectionId,
            @P("当前会话范围内的数据库或 catalog 名称") String databaseName,
            @P(value = "当前会话范围内的 schema 名称；数据库类型不使用 schema 时省略", required = false) String schemaName,
            @P("要执行的只读 SQL 语句列表。")
            List<String> sqls,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
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
        "价值：在权限已允许时执行写入 SQL；否则返回前端确认所需的结构化 payload。",
        "使用时机：写入 SQL 已最终确定，可以执行或需要明确确认。",
        "前置条件：sqls 必填，且只能包含写入语句。",
        "结果：EXECUTED 表示写入已执行；REQUIRES_CONFIRMATION 表示尚未执行，需要前端询问用户。",
        "边界：不要用于只读查询或猜测性的写入 SQL。"
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_NON_SELECT_SQL)
    public ExecuteNonSelectToolResult executeNonSelectSql(
            @P("当前会话范围内的连接 ID") Long connectionId,
            @P("当前会话范围内的数据库或 catalog 名称") String databaseName,
            @P(value = "当前会话范围内的 schema 名称；数据库类型不使用 schema 时省略", required = false) String schemaName,
            @P("要执行的非 SELECT SQL 语句列表，例如 INSERT、UPDATE、DELETE、DDL。")
            List<String> sqls,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
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
                        ? "写入 SQL 已基于默认允许权限执行。"
                        : "写入 SQL 已在用户明确确认后执行。"
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
            String currentError = StringUtils.defaultIfBlank(response.getErrorMessage(), "未知数据库错误");
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
