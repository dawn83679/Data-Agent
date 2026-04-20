package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
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
                return AgentToolResult.empty(buildEmptyMessage(connectionId, databaseName));
            }
            log.info("[Tool done] getSchemas connectionId={}, database={}, count={}",
                    connectionId, databaseName, schemas.size());
            return AgentToolResult.success(schemas, buildSuccessMessage(connectionId, databaseName, schemas.size()));
        } catch (Exception e) {
            log.warn("[Tool] getSchemas failed for connectionId={}, database={}: {}",
                    connectionId, databaseName, e.getMessage());
            return AgentToolResult.fail(buildFailureMessage(connectionId, databaseName, e.getMessage()));
        }
    }

    private String buildSuccessMessage(Long connectionId, String databaseName, int schemaCount) {
        return ToolMessageSupport.sentence(
                "Found " + schemaCount + " schema(s) in database '" + databaseName + "' on connection " + connectionId + ".",
                "Use askUserQuestion to ask the user which schema should be used before continuing."
        );
    }

    private String buildEmptyMessage(Long connectionId, String databaseName) {
        return ToolMessageSupport.sentence(
                "No schemas found in database '" + databaseName + "' on connection " + connectionId + ".",
                "Use askUserQuestion to ask the user whether another database should be used or whether schema scope can be skipped before continuing."
        );
    }

    private String buildFailureMessage(Long connectionId, String databaseName, String errorMessage) {
        return ToolMessageSupport.sentence(
                "Failed to get schemas in database '" + databaseName + "' on connection " + connectionId + ": " + errorMessage + ".",
                "Use askUserQuestion to ask the user whether to retry with another database or connection before continuing."
        );
    }
}
