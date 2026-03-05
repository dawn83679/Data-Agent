package edu.zsc.ai.agent.tool;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.model.UserQuestion;
import edu.zsc.ai.common.constant.RequestContextConstant;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified tool class for asking user questions and requesting write confirmation.
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class AskUserTool {

    private final WriteConfirmationStore confirmationStore;

    @Tool(
            value = "[WHAT] Ask the user one or more questions with structured choices and optional free-text input. "
                    + "[WHEN] Use when: (1) user intent is ambiguous or critical information is missing; "
                    + "(2) a decision must be made before proceeding. "
                    + "IMPORTANT — Use askUserConfirm (NOT askUserQuestion) for write operation confirmation. "
                    + "[HOW] Each question should have 2-3 options (maximum 3). "
                    + "Users can select options and/or provide custom input. "
                    + "Use allowMultiSelect=true for multi-select (checkboxes), false for single-select (radio buttons, default). "
                    + "After receiving the response, interpret the answers and continue the operation.",
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public List<UserQuestion> askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        log.info("[Tool] askUserQuestion, {} question(s)", questions == null ? 0 : questions.size());
        return questions;
    }

    @Tool(
            value = {
                    "[WHAT] Request user confirmation before executing a write SQL statement (INSERT, UPDATE, DELETE, DDL).",
                    "[WHEN] YOU MUST call this tool BEFORE every write operation. "
                            + "Use askUserQuestion ONLY for intent clarification when the request is ambiguous — "
                            + "NEVER use askUserQuestion for write operation confirmation.",
                    "[HOW] Pass the exact SQL, connection id, and a clear explanation of the operation's effect. "
                            + "Include database and schema only when the operation is bound to a specific database/schema; "
                            + "for example, omit them for CREATE DATABASE statements.",
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
            @P(value = "Database (catalog) name from current session context; omit or null for operations not bound to a specific database (e.g. CREATE DATABASE)", required = false)
            String databaseName,
            @P(value = "Schema name from current session context; omit or null if not applicable", required = false)
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

        WriteConfirmationEntry entry = confirmationStore.create(
                userId, conversationId, connectionId, sql, databaseName, schemaName);

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
