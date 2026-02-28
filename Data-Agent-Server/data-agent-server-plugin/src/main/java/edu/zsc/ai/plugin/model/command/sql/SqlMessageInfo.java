package edu.zsc.ai.plugin.model.command.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlMessageInfo {
    private SqlMessageLevel level;
    private String code;
    private String sqlState;
    private String message;
    private String detail;
}
