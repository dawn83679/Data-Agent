package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyInfo {
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
}
