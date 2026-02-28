package edu.zsc.ai.plugin.model.command.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlCommandSubResult {
    private boolean query;
    private int affectedRows;
    private Long executionMs;
    private Long fetchingMs;
    private List<String> headers;
    private List<List<Object>> rows;
    private List<SqlColumnInfo> columns;
    private Integer fetchRows;
    private Boolean truncated;
    private Boolean limitApplied;
    private List<SqlMessageInfo> messages;
}
