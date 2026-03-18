package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String tableName;
    private String comment;
    private Long approximateRowCount;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;
    private List<ForeignKeyInfo> foreignKeys;
    private List<IndexInfo> indexes;
}
