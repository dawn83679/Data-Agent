package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetSchemasTool {

    private final SchemaService schemaService;

    @Tool({
            "Value: returns the schemas within a specific database on a connection.",
            "Use When: you know the target connection and database, and need to discover available schemas (e.g., PostgreSQL).",
            "Note: MySQL does not use schemas — this tool may return empty for MySQL connections.",
            "After Success: use the schema to narrow searchObjects or getObjectDetail scope."
    })
    public AgentToolResult getSchemas(
            @P("The connection ID") Long connectionId,
            @P("The database (catalog) name") String databaseName,
            InvocationParameters parameters) {
        log.info("[Tool] getSchemas connectionId={}, database={}", connectionId, databaseName);
        try {
            List<String> schemas = schemaService.listSchemas(connectionId, databaseName);
            if (CollectionUtils.isEmpty(schemas)) {
                log.info("[Tool done] getSchemas -> empty");
                return AgentToolResult.empty(
                        "No schemas found in database '" + databaseName + "' on connection " + connectionId + ".");
            }
            log.info("[Tool done] getSchemas connectionId={}, database={}, count={}",
                    connectionId, databaseName, schemas.size());
            return AgentToolResult.success(schemas,
                    "Found " + schemas.size() + " schema(s) in database '" + databaseName
                            + "' on connection " + connectionId + ".");
        } catch (Exception e) {
            log.warn("[Tool] getSchemas failed for connectionId={}, database={}: {}",
                    connectionId, databaseName, e.getMessage());
            return AgentToolResult.fail(
                    "Failed to get schemas in database '" + databaseName
                            + "' on connection " + connectionId + ": " + e.getMessage());
        }
    }
}
