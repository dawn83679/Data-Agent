package edu.zsc.ai.plugin.model.sql;

import java.util.List;

public record SqlValidationResult(
    boolean valid,
    SqlType sqlType,
    List<SqlError> errors,
    List<String> tables,
    List<String> columns
) {
    public static SqlValidationResult valid(SqlType sqlType, List<String> tables, List<String> columns) {
        return new SqlValidationResult(true, sqlType, List.of(), tables, columns);
    }

    public static SqlValidationResult invalid(SqlType sqlType, List<SqlError> errors) {
        return new SqlValidationResult(false, sqlType, errors, List.of(), List.of());
    }

    public static SqlValidationResult typeOnly(SqlType sqlType) {
        return new SqlValidationResult(true, sqlType, List.of(), List.of(), List.of());
    }
}
