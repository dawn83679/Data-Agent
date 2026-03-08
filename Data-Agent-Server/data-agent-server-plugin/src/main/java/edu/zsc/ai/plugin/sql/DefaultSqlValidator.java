package edu.zsc.ai.plugin.sql;

import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.model.sql.SqlValidationResult;

/**
 * Default fallback SqlValidator when a plugin does not provide its own implementation.
 * Uses keyword-based classification without full syntax validation.
 */
public class DefaultSqlValidator implements SqlValidator {

    public static final DefaultSqlValidator INSTANCE = new DefaultSqlValidator();

    @Override
    public SqlValidationResult validate(String sql) {
        SqlType type = classifySql(sql);
        return SqlValidationResult.typeOnly(type);
    }

    @Override
    public SqlType classifySql(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlType.UNKNOWN;
        }
        String stripped = stripLeadingComments(sql).stripLeading();
        if (stripped.isEmpty()) {
            return SqlType.UNKNOWN;
        }
        String firstWord = stripped.split("\\s+")[0].toUpperCase();
        return switch (firstWord) {
            case "SELECT", "WITH" -> SqlType.SELECT;
            case "INSERT" -> SqlType.INSERT;
            case "UPDATE" -> SqlType.UPDATE;
            case "DELETE" -> SqlType.DELETE;
            case "MERGE" -> SqlType.MERGE;
            case "CREATE" -> SqlType.CREATE;
            case "ALTER" -> SqlType.ALTER;
            case "DROP" -> SqlType.DROP;
            case "TRUNCATE" -> SqlType.TRUNCATE;
            case "GRANT" -> SqlType.GRANT;
            case "REVOKE" -> SqlType.REVOKE;
            case "SHOW" -> SqlType.SHOW;
            case "EXPLAIN" -> SqlType.EXPLAIN;
            case "DESCRIBE", "DESC" -> SqlType.DESCRIBE;
            case "USE" -> SqlType.USE;
            case "SET" -> SqlType.SET;
            case "BEGIN", "START" -> SqlType.BEGIN;
            case "COMMIT" -> SqlType.COMMIT;
            case "ROLLBACK" -> SqlType.ROLLBACK;
            default -> SqlType.UNKNOWN;
        };
    }

    private String stripLeadingComments(String sql) {
        String s = sql.stripLeading();
        while (!s.isEmpty()) {
            if (s.startsWith("--")) {
                int nl = s.indexOf('\n');
                s = (nl == -1) ? "" : s.substring(nl + 1).stripLeading();
            } else if (s.startsWith("/*")) {
                int end = s.indexOf("*/");
                s = (end == -1) ? "" : s.substring(end + 2).stripLeading();
            } else {
                break;
            }
        }
        return s;
    }
}
