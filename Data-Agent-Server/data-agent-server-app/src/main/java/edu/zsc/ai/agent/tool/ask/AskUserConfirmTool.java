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
                    "Shows the user exactly what a write operation will do and gets explicit approval. ",
                    "Server enforces this — executeNonSelectSql rejects writes without a valid token.",
                    "",
                    "Always pass: finalized SQL, connectionId, and a clear impact explanation ",
                    "(what changes, estimated affected rows, cascade effects).",
                    "",
                    "CRITICAL: Mandatory before every write operation. No exceptions."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public WriteConfirmationResult askUserConfirm(
            @P("The exact SQL statement to be executed (INSERT, UPDATE, DELETE, or DDL)")
            String sql,
            @P("Connection id")
            Long connectionId,
            @P(value = "Database name; omit for server-level operations", required = false)
            String databaseName,
            @P(value = "Schema name; omit if N/A", required = false)
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
