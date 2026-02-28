package edu.zsc.ai.plugin.model.command.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlColumnInfo {
    private String name;
    private String label;
    private String typeName;
    private Integer jdbcType;
    private Integer precision;
    private Integer scale;
    private Boolean nullable;
    private String tableName;
}
