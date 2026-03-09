package edu.zsc.ai.agent.tool.ask;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.domain.model.context.DbContext;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for requesting explicit user approval before write SQL operations.
 * DISABLED in Plan mode (filtered out from tool list).
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class AskUserConfirmTool {

    private final WriteConfirmationStore confirmationStore;

    @Tool(
            value = {
                    "The critical safety net that prevents irreversible data damage — shows the user ",
                    "exactly what will change and gets explicit approval before any write executes. ",
                    "This single step has prevented countless accidental DELETEs and wrong UPDATEs.",
                    "",
                    "Mandatory before every write operation: the server enforces this by rejecting ",
                    "executeNonSelectSql without a valid confirmation token. Always pass the finalized ",
                    "SQL, connectionId, and a clear impact explanation so the user can make an ",
                    "informed decision."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public WriteConfirmationResult askUserConfirm(
            @P("The exact SQL statement to be executed (INSERT, UPDATE, DELETE, or DDL)")
            String sql,
            @P("Connection id from current session context")
            Long connectionId,
            @P(value = "Database (catalog) name from current session context; omit or null for operations not bound to a specific database (e.g. CREATE DATABASE)", required = false)
            String databaseName,
            @P(value = "Schema name from current session context; omit or null if not applicable", required = false)
            String schemaName,
            @P("Brief explanation of what this operation does and its potential impact")
            String explanation,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] askUserConfirm, connectionId={}, database={}, schema={}, sqlLength={}",
                    connectionId, databaseName, schemaName, sql != null ? sql.length() : 0);

            DbContext db = new DbContext(connectionId, databaseName, schemaName);
            WriteConfirmationEntry entry = confirmationStore.create(db, sql);

            log.info("[Tool done] askUserConfirm, token={}", entry.getToken());
            return WriteConfirmationResult.builder()
                    .confirmationToken(entry.getToken())
                    .expiresInSeconds(300)
                    .build();
        }
    }

    /**
     * Result returned to the frontend via SSE (serialized as JSON).
     * The frontend parses this as WriteConfirmPayload.
     */
    @Data
    @Builder
    public static class WriteConfirmationResult {
        private String confirmationToken;
        private String sqlPreview;
        private String explanation;
        private Long connectionId;
        private String databaseName;
        private String schemaName;
        private long expiresInSeconds;
        private boolean error;
        private String errorMessage;

        public static WriteConfirmationResult error(String message) {
            return WriteConfirmationResult.builder()
                    .error(true)
                    .errorMessage(message)
                    .build();
        }
    }
}
