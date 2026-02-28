package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Objects;

import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class TableTool {

    private final TableService tableService;


    @Tool({
        "[WHAT] Search for table names in the current database/schema using a JDBC-style name pattern.",
        "[WHEN] Use when you need to find tables by name. Pass connectionId, databaseName, schemaName from current session context.",
        "IMPORTANT — ALWAYS call countTables first. If count exceeds 50, use a specific pattern to narrow scope instead of listing all.",
        "[HOW] Supports '%' (any sequence of chars) and '_' (single char) wildcards. Pass null or '%' to match all tables."
    })
    public String searchTables(
            @P(value = "Table name pattern for fuzzy search. Supports '%' and '_' wildcards. Pass null or '%' to list all tables.", required = false) String tableNamePattern,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} searchTables, pattern={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableNamePattern, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> tables = tableService.searchTables(connectionId, databaseName, schemaName, tableNamePattern, userId);

            if (CollectionUtils.isEmpty(tables)) {
                log.info("{} searchTables -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_TABLES);
                return ToolMessageConstants.EMPTY_NO_TABLES;
            }

            log.info("{} searchTables, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, tables.size());
            return tables.toString();
        } catch (Exception e) {
            log.error("{} searchTables", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Count the number of tables matching a name pattern in the current database/schema.",
        "[WHEN] ALWAYS call this before searchTables to assess schema size.",
        "IMPORTANT — If the count exceeds 50, MUST use a specific tableNamePattern in searchTables instead of listing all tables."
    })
    public String countTables(
            @P(value = "Table name pattern. Supports '%' and '_' wildcards. Pass null or '%' to count all tables.", required = false) String tableNamePattern,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} countTables, pattern={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableNamePattern, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> tables = tableService.searchTables(connectionId, databaseName, schemaName, tableNamePattern, userId);
            int count = tables.size();
            log.info("{} countTables, pattern={}, count={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, tableNamePattern, count);
            return String.valueOf(count);
        } catch (Exception e) {
            log.error("{} countTables", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Count the total number of rows in a specific table.",
        "[WHEN] ALWAYS call this before executing any SELECT query to assess data volume.",
        "IMPORTANT — If the row count exceeds 10000, MUST add a WHERE clause or LIMIT. NEVER run an unfiltered SELECT on a large table."
    })
    public String countTableRows(
            @P("The exact name of the table to count rows in") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} countTableRows, tableName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            long count = tableService.countTableRows(connectionId, databaseName, schemaName, tableName, userId);
            log.info("{} countTableRows, tableName={}, count={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, tableName, count);
            return String.valueOf(count);
        } catch (Exception e) {
            log.error("{} countTableRows, tableName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, tableName, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Get the CREATE TABLE DDL for a specific table.",
        "[WHEN] Use when you need to understand a table's column names, types, constraints, or keys before generating SQL. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getTableDdl(
            @P("The exact name of the table in the current schema") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTableDdl, tableName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            String ddl = tableService.getTableDdl(connectionId, databaseName, schemaName, tableName, userId);

            log.info("{} getTableDdl, tableName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    tableName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getTableDdl, tableName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, tableName, e);
            return e.getMessage();
        }
    }
}
