package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.model.command.CommandResult;
import edu.zsc.ai.plugin.model.command.base.BaseCommandResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlCommandResult extends BaseCommandResult implements CommandResult {
    private String originalSql;

    private String executedSql;

    private int affectedRows;

    private boolean isQuery;

    private Long executionMs;

    private Long fetchingMs;

    private Integer errorCode;

    private String sqlState;

    private String errorDetail;

    private List<String> headers;

    private List<List<Object>> rows;

    private List<SqlColumnInfo> columns;

    private Long startTime;

    private Long endTime;

    private Integer fetchRows;

    private Boolean truncated;

    private Boolean limitApplied;

    private List<SqlMessageInfo> messages;

    private List<SqlCommandSubResult> results;

    /**
     * Get value from a row by column name. Use this instead of row.get(index) to avoid
     * magic indices when column order may vary.
     *
     * @param row        the row (must match headers order)
     * @param columnName column name from headers (case-sensitive)
     * @return the value, or null if column not found or row is null
     */
    public Object getValueByColumnName(List<Object> row, String columnName) {
        if (headers == null || row == null || columnName == null) {
            return null;
        }
        int idx = headers.indexOf(columnName);
        return idx >= 0 && idx < row.size() ? row.get(idx) : null;
    }

}
