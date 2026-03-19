package edu.zsc.ai.agent.tool.message;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared message helpers for SQL tools.
 */
public final class SqlToolMessageSupport {

    private SqlToolMessageSupport() {
    }

    public static String buildScope(Long connectionId, String databaseName, String schemaName) {
        List<String> parts = new ArrayList<>();
        parts.add("connectionId=" + connectionId);
        if (StringUtils.isNotBlank(databaseName)) {
            parts.add("database=" + databaseName);
        }
        if (StringUtils.isNotBlank(schemaName)) {
            parts.add("schema=" + schemaName);
        }
        return String.join(", ", parts);
    }

    public static String requireReadOnlyStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeSelectSql only accepts read-only statements for " + buildScope(connectionId, databaseName, schemaName) + ".",
                "Move INSERT, UPDATE, DELETE, and DDL statements to executeNonSelectSql.",
                "Do not continue with executeSelectSql until every statement is read-only."
        );
    }

    public static String requireReadStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeSelectSql requires at least one read-only SQL statement for " + buildScope(connectionId, databaseName, schemaName) + ".",
                "Provide SELECT, WITH, SHOW, or EXPLAIN statements before retrying."
        );
    }

    public static String requireWriteStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeNonSelectSql requires at least one write statement for " + buildScope(connectionId, databaseName, schemaName) + ".",
                "Finalize the write SQL and retry."
        );
    }

    public static String confirmationRequired(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeNonSelectSql requires user confirmation for " + buildScope(connectionId, databaseName, schemaName) + ".",
                "Nothing has been executed yet.",
                "Wait for the user to confirm and then retry executeNonSelectSql with the exact same SQL."
        );
    }

    public static String failureMessage(boolean writeOperation,
                                        Long connectionId,
                                        String databaseName,
                                        String schemaName,
                                        int statementIndex,
                                        int statementCount,
                                        String sqlPreview,
                                        String currentError) {
        String operation = writeOperation ? "Write SQL" : "Read SQL";
        String statementLabel = statementCount > 1 ? "statement " + (statementIndex + 1) : "the statement";
        String preview = StringUtils.isNotBlank(sqlPreview) ? " (" + sqlPreview + ")" : "";
        String guidance = writeOperation
                ? "Review the statement, constraints, and permissions before retrying. Do not retry a write blindly."
                : "Review the SQL text and schema context before retrying. If the target object is still unclear, return to discovery first.";
        return ToolMessageSupport.sentence(
                operation + " failed for " + buildScope(connectionId, databaseName, schemaName)
                        + " at " + statementLabel + preview + ".",
                "Error: " + currentError + ".",
                guidance
        );
    }
}
