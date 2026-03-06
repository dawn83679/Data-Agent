package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlExecutionInfo;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponseType;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResultSet;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlSubResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Slim SQL execution result for agent consumption.
 * Strips fields the agent already knows (sql, databaseName, schemaName)
 * and removes redundant legacy/timing fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSqlResult {

    private boolean success;
    private String error;

    /** RESULT_SET / UPDATE_COUNT / DDL_RESULT etc. */
    private String type;

    private List<AgentSqlColumn> columns;
    private List<List<Object>> rows;

    /** Rows affected (for DML) or null for SELECT */
    private Integer affectedRows;

    /** Whether result data was truncated by server or client limit */
    private Boolean truncated;

    /** Whether an automatic row LIMIT was applied */
    private Boolean limitApplied;

    /** Populated only for multi-statement SQL */
    private List<AgentSqlResult> results;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static AgentSqlResult fail(String error) {
        return AgentSqlResult.builder().success(false).error(error).build();
    }

    public static AgentSqlResult from(ExecuteSqlResponse r) {
        if (!r.isSuccess()) {
            return fail(r.getErrorMessage());
        }
        // multi-statement path
        if (r.getResults() != null && !r.getResults().isEmpty()) {
            List<AgentSqlResult> subResults = r.getResults().stream()
                    .map(AgentSqlResult::fromSub)
                    .toList();
            return AgentSqlResult.builder().success(true).results(subResults).build();
        }
        // new structured single-statement path
        if (r.getResultSet() != null) {
            return fromResultSet(r.getType(), r.getResultSet(), r.getExecutionInfo());
        }
        // legacy fallback
        return fromLegacy(r);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static AgentSqlResult fromSub(ExecuteSqlSubResult sub) {
        if (sub.getResultSet() != null) {
            return fromResultSet(sub.getType(), sub.getResultSet(), null);
        }
        List<AgentSqlColumn> cols = sub.getHeaders() == null ? null :
                sub.getHeaders().stream().map(h -> new AgentSqlColumn(h, null, null)).toList();
        return AgentSqlResult.builder()
                .success(true)
                .type(sub.getType() != null ? sub.getType().name() : null)
                .columns(cols)
                .rows(sub.getRows())
                .affectedRows(sub.getAffectedRows() > 0 ? sub.getAffectedRows() : null)
                .build();
    }

    private static AgentSqlResult fromResultSet(ExecuteSqlResponseType type,
                                                ExecuteSqlResultSet rs,
                                                ExecuteSqlExecutionInfo info) {
        List<AgentSqlColumn> columns = rs.getColumns() == null ? null :
                rs.getColumns().stream()
                        .map(c -> new AgentSqlColumn(c.getName(), c.getTypeName(), c.getNullable()))
                        .toList();
        return AgentSqlResult.builder()
                .success(true)
                .type(type != null ? type.name() : null)
                .columns(columns)
                .rows(rs.getRows())
                .affectedRows(info != null ? info.getAffectedRows() : null)
                .truncated(rs.getTruncated())
                .limitApplied(info != null ? info.getLimitApplied() : null)
                .build();
    }

    private static AgentSqlResult fromLegacy(ExecuteSqlResponse r) {
        List<AgentSqlColumn> cols = r.getHeaders() == null ? null :
                r.getHeaders().stream().map(h -> new AgentSqlColumn(h, null, null)).toList();
        return AgentSqlResult.builder()
                .success(true)
                .type(r.isQuery() ? "RESULT_SET" : "UPDATE_COUNT")
                .columns(cols)
                .rows(r.getRows())
                .affectedRows(r.getAffectedRows() > 0 ? r.getAffectedRows() : null)
                .build();
    }
}
