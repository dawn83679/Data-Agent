package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConsumeResult;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecuteSqlToolTest {

    private final SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
    private final WriteConfirmationStore writeConfirmationStore = mock(WriteConfirmationStore.class);
    private final ExecuteSqlTool tool = new ExecuteSqlTool(sqlExecutionService, writeConfirmationStore);

    @Test
    void executeSelectSql_rejectsEmptyStatementsWithGuidance() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.executeSelectSql(5L, "sales", "public", List.of(), InvocationParameters.from(Map.of()))
        );

        assertTrue(exception.getMessageForModel().contains("executeSelectSql requires at least one read-only SQL statement"));
        assertTrue(exception.getMessageForModel().contains("connectionId=5, database=sales, schema=public"));
        assertTrue(exception.getMessageForModel().contains("Provide SELECT, WITH, SHOW, or EXPLAIN statements before retrying"));
    }

    @Test
    void executeNonSelectSql_reportsConfirmationBlockWithContext() {
        when(writeConfirmationStore.consumeConfirmedBySql(any(), any()))
                .thenReturn(WriteConsumeResult.fail("NOT_CONFIRMED",
                        "A confirmation token exists, but the user has not confirmed it yet. Wait for user confirmation before executing write SQL."));

        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.executeNonSelectSql(
                        5L,
                        "sales",
                        "public",
                        List.of("DELETE FROM orders WHERE id = 1"),
                        InvocationParameters.from(Map.of()))
        );

        assertTrue(exception.getMessageForModel().contains("executeNonSelectSql is blocked for connectionId=5, database=sales, schema=public"));
        assertTrue(exception.getMessageForModel().contains("Wait for user confirmation before executing write SQL"));
        assertTrue(exception.getMessageForModel().contains("Call askUserConfirm with the exact final SQL and wait for approval before retrying"));
    }

    @Test
    void executeNonSelectSql_rewritesDatabaseFailureIntoGuidanceMessage() {
        when(writeConfirmationStore.consumeConfirmedBySql(any(), any())).thenReturn(WriteConsumeResult.ok());
        when(sqlExecutionService.executeBatchSql(any(), any())).thenReturn(List.of(
                ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("duplicate key value violates unique constraint")
                        .build()
        ));

        AgentSqlResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("INSERT INTO orders(id) VALUES (1)"),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getResults() != null && result.getResults().size() == 1);
        AgentSqlResult failedStatement = result.getResults().get(0);
        assertFalse(failedStatement.isSuccess());
        assertTrue(failedStatement.getError().contains("Write SQL failed for connectionId=5, database=sales, schema=public"));
        assertTrue(failedStatement.getError().contains("INSERT INTO orders(id) VALUES (1)"));
        assertTrue(failedStatement.getError().contains("duplicate key value violates unique constraint"));
        assertTrue(failedStatement.getError().contains("Do not retry a write blindly"));
    }
}
