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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class WriteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "Executes write SQL (INSERT, UPDATE, DELETE, DDL). Requires a valid confirmation token ",
        "from askUserConfirm — server rejects writes without prior confirmation.",
        "",
        "Accepts a list of statements. Results returned per statement.",
        "",
        "Use when: executing approved write operations.",
        "NEVER: call without prior askUserConfirm approval."
    })
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id") Long connectionId,
            @P("Database (catalog) name") String databaseName,
            @P(value = "Schema name; omit if N/A", required = false) String schemaName,
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
}
