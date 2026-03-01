package edu.zsc.ai.agent.tool;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.common.constant.RequestContextConstant;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool that generates a secure write-confirmation token and immediately suspends
 * the agent run so the user can review and confirm the SQL in the UI.
 *
 * Flow:
 * 1. Agent calls askUserConfirm → token created (PENDING) → SSE ends (IMMEDIATE).
 * 2. Frontend shows SQL preview + Confirm/Cancel buttons.
 * 3. User clicks Confirm → frontend POSTs /api/chat/write-confirm/confirm → token becomes CONFIRMED.
 * 4. Frontend calls submitMessage("Confirmed. confirmationToken: <token>").
 * 5. Agent resumes in Run 2 → calls executeNonSelectSql(sql, confirmationToken).
 * 6. Server validates token is CONFIRMED, consumes it, executes SQL.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AskUserConfirmTool {

    private final WriteConfirmationStore confirmationStore;

    @Tool(
            value = {
                "[WHAT] Request user confirmation before executing a write SQL statement (INSERT, UPDATE, DELETE, DDL).",
                "[WHEN] YOU MUST call this tool BEFORE every write operation. "
                    + "Use askUserQuestion ONLY for intent clarification when the request is ambiguous — "
                    + "NEVER use askUserQuestion for write operation confirmation.",
                "[HOW] Pass the exact SQL, target database, and a clear explanation of the operation's effect. "
                    + "The tool generates a secure confirmation token. "
                    + "The user will review the SQL preview in the UI and click 'Confirm & Execute'.",
                "[AFTER] After the user confirms, you will receive a message like "
                    + "'Confirmed. confirmationToken: <token>'. "
                    + "Then call executeNonSelectSql with the sql AND the confirmationToken. "
                    + "NEVER call executeNonSelectSql without a valid confirmationToken from this tool."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public WriteConfirmationResult askUserConfirm(
            @P("The exact SQL statement to be executed (INSERT, UPDATE, DELETE, or DDL)")
            String sql,
            @P("Connection id from current session context")
            Long connectionId,
            @P("Database (catalog) name from current session context")
            String databaseName,
            @P(value = "Schema name from current session context; omit if not applicable", required = false)
            String schemaName,
            @P("Brief explanation of what this operation does and its potential impact")
            String explanation,
            InvocationParameters parameters) {

        log.info("[Tool] askUserConfirm, connectionId={}, database={}, schema={}, sqlLength={}",
                connectionId, databaseName, schemaName, sql != null ? sql.length() : 0);

        Long userId = parameters.get(RequestContextConstant.USER_ID);
        Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);

        if (userId == null || conversationId == null) {
            log.error("[Tool] askUserConfirm: missing context userId={} conversationId={}", userId, conversationId);
            return WriteConfirmationResult.error("User or conversation context is missing.");
        }

        WriteConfirmationEntry entry = confirmationStore.create(userId, conversationId, connectionId,sql, databaseName, schemaName);

        log.info("[Tool done] askUserConfirm, token={}", entry.getToken());
        return WriteConfirmationResult.builder()
                .confirmationToken(entry.getToken())
                .sqlPreview(sql)
                .explanation(explanation)
                .connectionId(connectionId)
                .databaseName(databaseName)
                .schemaName(schemaName)
                .expiresInSeconds(300)
                .build();
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
