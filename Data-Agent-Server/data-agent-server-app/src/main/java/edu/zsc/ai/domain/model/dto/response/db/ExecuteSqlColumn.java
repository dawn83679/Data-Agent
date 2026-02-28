package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlColumn {
    private String name;
    private String label;
    private String typeName;
    private Integer jdbcType;
    private Integer precision;
    private Integer scale;
    private Boolean nullable;
    private String tableName;
}
