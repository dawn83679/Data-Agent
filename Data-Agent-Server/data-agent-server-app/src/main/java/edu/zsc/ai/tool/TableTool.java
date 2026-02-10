package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class TableTool {

    private final TableService tableService;


    @Tool({
        "Get the list of all table names in the current database/schema.",
        "Use when the user asks what tables exist or to explore the schema."
    })
    public String getTableNames(InvocationParameters parameters) {
        log.info("[Tool before] getTableNames");
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return "User context missing.";
            }
            Long connectionId = parameters.get(RequestContextConstant.CONNECTION_ID);
            String databaseName = parameters.get(RequestContextConstant.DATABASE_NAME);
            String schemaName = parameters.get(RequestContextConstant.SCHEMA_NAME);
            log.info("Tool: getTableNames called with context: connectionId={}, database={}, schema={}",
                    connectionId, databaseName, schemaName);

            List<String> tables = tableService.listTables(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (tables == null || tables.isEmpty()) {
                log.info("[Tool done] getTableNames -> EMPTY: No tables found.");
                return "EMPTY: No tables found.";
            }

            log.info("[Tool done] getTableNames, result size={}", tables.size());
            return tables.toString();
        } catch (Exception e) {
            log.error("[Tool error] getTableNames", e);
            return e.getMessage();
        }
    }

  
    @Tool({
        "Get the DDL (Data Definition Language) statement for a specific table.",
        "Use when the user needs the table definition or CREATE TABLE statement."
    })
    public String getTableDdl(@P("The exact name of the table in the current schema") String tableName,
                             InvocationParameters parameters) {
        log.info("[Tool before] getTableDdl, tableName={}", tableName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return "User context missing.";
            }
            Long connectionId = parameters.get(RequestContextConstant.CONNECTION_ID);
            String databaseName = parameters.get(RequestContextConstant.DATABASE_NAME);
            String schemaName = parameters.get(RequestContextConstant.SCHEMA_NAME);
            log.info("Tool: getTableDdl called for table: {} with context: connectionId={}, database={}, schema={}",
                    tableName, connectionId, databaseName, schemaName);

            String ddl = tableService.getTableDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    tableName,
                    userId
            );

            log.info("[Tool done] getTableDdl, tableName={}, ddlLength={}", tableName, ddl != null ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("[Tool error] getTableDdl, tableName={}", tableName, e);
            return e.getMessage();
        }
    }
}
