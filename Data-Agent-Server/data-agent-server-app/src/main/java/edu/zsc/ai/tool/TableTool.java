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
        "Get the list of all table names in the current database/schema.",
        "Use when the user asks what tables exist or to explore the schema. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getTableNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTableNames, connectionId={}, database={}, schema={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE,
                connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> tables = tableService.getTables(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (CollectionUtils.isEmpty(tables)) {
                log.info("{} getTableNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_TABLES);
                return ToolMessageConstants.EMPTY_NO_TABLES;
            }

            log.info("{} getTableNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, tables.size());
            return tables.toString();
        } catch (Exception e) {
            log.error("{} getTableNames", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "Get the DDL (Data Definition Language) statement for a specific table.",
        "Use when the user needs the table definition or CREATE TABLE statement. Pass connectionId, databaseName, schemaName from current session context."
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
            String ddl = tableService.getTableDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    tableName,
                    userId
            );

            log.info("{} getTableDdl, tableName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    tableName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getTableDdl, tableName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, tableName, e);
            return e.getMessage();
        }
    }
}
