package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.service.db.TableService;
import edu.zsc.ai.util.ToolResultFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

 
@Component
@Slf4j
@RequiredArgsConstructor
public class TableTool {

    private final TableService tableService;

    /**
     * Get all table names in the current database/schema.
     */
    @Tool("Get the list of all table names in the current database/schema")
    public String getTableNames() {
        try {
            log.info("Tool: getTableNames called with context: connectionId={}, database={}, schema={}",
                    RequestContext.getConnectionId(), RequestContext.getDatabaseName(), RequestContext.getSchemaName());
            
            List<String> tables = tableService.listTables(
                    RequestContext.getConnectionId(),
                    RequestContext.getDatabaseName(),
                    RequestContext.getSchemaName()
            );
            
            if (tables == null || tables.isEmpty()) {
                return ToolResultFormatter.empty("No tables found.");
            }
            
            return ToolResultFormatter.success(tables.toString());
        } catch (Exception e) {
            log.error("Error in getTableNames tool", e);
            return ToolResultFormatter.error(e.getMessage());
        }
    }

    /**
     * Get the DDL for a specific table.
     */
    @Tool("Get the DDL (Data Definition Language) statement for a specific table")
    public String getTableDdl(@P("The name of the table") String tableName) {
        try {
            log.info("Tool: getTableDdl called for table: {} with context: connectionId={}, database={}, schema={}",
                    tableName, RequestContext.getConnectionId(), RequestContext.getDatabaseName(), RequestContext.getSchemaName());
            
            String ddl = tableService.getTableDdl(
                    RequestContext.getConnectionId(),
                    RequestContext.getDatabaseName(),
                    RequestContext.getSchemaName(),
                    tableName
            );
            
            return ToolResultFormatter.success(ddl);
        } catch (Exception e) {
            log.error("Error in getTableDdl tool for table: " + tableName, e);
            return ToolResultFormatter.error();
        }
    }
}
