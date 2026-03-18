package edu.zsc.ai.agent.tool.ask.confirm;

import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.context.DbContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteConfirmationStoreTest {

    private final WriteConfirmationStore store = new WriteConfirmationStore();

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void consumeConfirmedBySql_reportsMissingTokenGuidance() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());

        WriteConsumeResult result = store.consumeConfirmedBySql(new DbContext(5L, "sales", "public"), "DELETE FROM orders");

        assertFalse(result.success());
        assertEquals("NO_TOKEN", result.reason());
        assertTrue(result.detail().contains("No confirmation token exists for this conversation"));
        assertTrue(result.detail().contains("You must call askUserConfirm first"));
    }

    @Test
    void consumeConfirmedBySql_reportsPendingConfirmationGuidance() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        store.create(new DbContext(5L, "sales", "public"), "DELETE FROM orders");

        WriteConsumeResult result = store.consumeConfirmedBySql(new DbContext(5L, "sales", "public"), "DELETE FROM orders");

        assertFalse(result.success());
        assertEquals("NOT_CONFIRMED", result.reason());
        assertTrue(result.detail().contains("user has not confirmed it yet"));
        assertTrue(result.detail().contains("Wait for user confirmation before executing write SQL"));
    }

    @Test
    void consumeConfirmedBySql_reportsScopeMismatchWithConcreteContext() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        WriteConfirmationEntry entry = store.create(new DbContext(5L, "sales", "public"), "DELETE FROM orders");
        assertTrue(store.confirm(entry.getToken()));

        WriteConsumeResult result = store.consumeConfirmedBySql(new DbContext(9L, "sales", "public"), "DELETE FROM orders");

        assertFalse(result.success());
        assertEquals("PARAM_MISMATCH", result.reason());
        assertTrue(result.detail().contains("expects connectionId=5"));
        assertTrue(result.detail().contains("received connectionId=9"));
        assertTrue(result.detail().contains("Use the same connectionId"));
    }

    @Test
    void consumeConfirmedBySql_reportsSqlMismatchWithConfirmedAndReceivedSql() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        WriteConfirmationEntry entry = store.create(new DbContext(5L, "sales", "public"), "DELETE FROM orders WHERE id = 1");
        assertTrue(store.confirm(entry.getToken()));

        WriteConsumeResult result = store.consumeConfirmedBySql(
                new DbContext(5L, "sales", "public"),
                "DELETE FROM orders WHERE id = 2"
        );

        assertFalse(result.success());
        assertEquals("SQL_MISMATCH", result.reason());
        assertTrue(result.detail().contains("Confirmed: 'DELETE FROM orders WHERE id = 1'"));
        assertTrue(result.detail().contains("Received: 'DELETE FROM orders WHERE id = 2'"));
        assertTrue(result.detail().contains("You must call askUserConfirm again with the new SQL"));
    }
}
