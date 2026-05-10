package edu.zsc.ai.agent.tool.message;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SqlToolMessageSupport {

    private SqlToolMessageSupport() {
    }

    public static String buildScope(Long connectionId, String databaseName, String schemaName) {
        List<String> parts = new ArrayList<>();
        parts.add("connectionId=" + connectionId);
        if (StringUtils.isNotBlank(databaseName)) {
            parts.add("数据库=" + databaseName);
        }
        if (StringUtils.isNotBlank(schemaName)) {
            parts.add("schema=" + schemaName);
        }
        return String.join(", ", parts);
    }

    public static String requireReadOnlyStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeSelectSql 只接受只读语句，当前范围：" + buildScope(connectionId, databaseName, schemaName) + "。",
                "INSERT、UPDATE、DELETE 和 DDL 语句应改用 executeNonSelectSql。",
                "所有语句确认只读前，不要继续调用 executeSelectSql。"
        );
    }

    public static String requireReadStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeSelectSql 至少需要一条只读 SQL，当前范围：" + buildScope(connectionId, databaseName, schemaName) + "。",
                "重试前提供 SELECT、WITH、SHOW 或 EXPLAIN 语句。"
        );
    }

    public static String requireWriteStatements(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeNonSelectSql 至少需要一条写入 SQL，当前范围：" + buildScope(connectionId, databaseName, schemaName) + "。",
                "先确定写入 SQL，再重试。"
        );
    }

    public static String confirmationRequired(Long connectionId, String databaseName, String schemaName) {
        return ToolMessageSupport.sentence(
                "executeNonSelectSql 需要用户确认，当前范围：" + buildScope(connectionId, databaseName, schemaName) + "。",
                "目前没有执行任何 SQL。",
                "等待用户确认后，用完全相同的 SQL 重试 executeNonSelectSql。"
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
        String operation = writeOperation ? "写入 SQL" : "只读 SQL";
        String statementLabel = statementCount > 1 ? "第 " + (statementIndex + 1) + " 条语句" : "该语句";
        String preview = StringUtils.isNotBlank(sqlPreview) ? " (" + sqlPreview + ")" : "";
        String guidance = writeOperation
                ? "重试前检查语句、约束和权限，不要盲目重试写入。"
                : "重试前检查 SQL 文本和 schema 上下文；如果目标对象仍不清楚，先回到发现阶段。";
        return ToolMessageSupport.sentence(
                operation + "执行失败，范围：" + buildScope(connectionId, databaseName, schemaName)
                        + "，位置：" + statementLabel + preview + "。",
                "错误：" + currentError + "。",
                guidance
        );
    }
}
