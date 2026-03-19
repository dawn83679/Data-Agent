package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.sql.approval.WriteExecutionApprovalStore;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.agent.tool.sql.model.ExecuteNonSelectToolResult;
import edu.zsc.ai.agent.tool.sql.model.WriteExecutionGrantOption;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.domain.service.permission.PermissionRuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecuteSqlToolTest {

    private final SqlExecutionService sqlExecutionService = mock(SqlExecutionService.class);
    private final PermissionRuleService permissionRuleService = mock(PermissionRuleService.class);
    private final WriteExecutionApprovalStore writeExecutionApprovalStore = new WriteExecutionApprovalStore();
    private final ExecuteSqlTool tool = new ExecuteSqlTool(sqlExecutionService, permissionRuleService, writeExecutionApprovalStore);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void executeSelectSql_rejectsEmptyStatementsWithGuidance() {
        AgentToolExecuteException exception = org.junit.jupiter.api.Assertions.assertThrows(
                AgentToolExecuteException.class,
                () -> tool.executeSelectSql(5L, "sales", "public", List.of(), InvocationParameters.from(Map.of()))
        );

        assertTrue(exception.getMessageForModel().contains("executeSelectSql requires at least one read-only SQL statement"));
        assertTrue(exception.getMessageForModel().contains("connectionId=5, database=sales, schema=public"));
        assertTrue(exception.getMessageForModel().contains("Provide SELECT, WITH, SHOW, or EXPLAIN statements before retrying"));
    }

    @Test
    void executeNonSelectSql_returnsConfirmationPayloadWhenNoPermissionOrApproval() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        when(permissionRuleService.matchesEnabledRule(5L, "sales", "public")).thenReturn(false);

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("DELETE FROM orders WHERE id = 1"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(ExecuteNonSelectToolResult.Status.REQUIRES_CONFIRMATION, result.getStatus());
        assertTrue(result.isRequiresConfirmation());
        assertFalse(result.isRuleMatched());
        assertNotNull(result.getConfirmation());
        assertEquals(42L, result.getConfirmation().getConversationId());
        assertEquals(5L, result.getConfirmation().getConnectionId());
        assertEquals("sales", result.getConfirmation().getDatabaseName());
        assertEquals("public", result.getConfirmation().getSchemaName());
        assertEquals("DELETE FROM orders WHERE id = 1", result.getConfirmation().getSql());
        assertEquals("DELETE FROM orders WHERE id = 1", result.getConfirmation().getSqlPreview());
        assertEquals(List.of(
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.EXACT_SCHEMA),
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.DATABASE_ALL_SCHEMAS),
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.CONNECTION_ALL_DATABASES),
                        option(PermissionScopeType.USER, PermissionGrantPreset.EXACT_SCHEMA),
                        option(PermissionScopeType.USER, PermissionGrantPreset.DATABASE_ALL_SCHEMAS),
                        option(PermissionScopeType.USER, PermissionGrantPreset.CONNECTION_ALL_DATABASES)
                ),
                result.getConfirmation().getAvailableGrantOptions());
        assertNull(result.getExecution());
    }

    @Test
    void executeNonSelectSql_omitsExactSchemaGrantWhenSchemaIsMissing() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        when(permissionRuleService.matchesEnabledRule(5L, "sales", null)).thenReturn(false);

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                null,
                List.of("DELETE FROM orders WHERE id = 1"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(List.of(
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.DATABASE_ALL_SCHEMAS),
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.CONNECTION_ALL_DATABASES),
                        option(PermissionScopeType.USER, PermissionGrantPreset.DATABASE_ALL_SCHEMAS),
                        option(PermissionScopeType.USER, PermissionGrantPreset.CONNECTION_ALL_DATABASES)
                ),
                result.getConfirmation().getAvailableGrantOptions());
    }

    @Test
    void executeNonSelectSql_onlyOffersConnectionGrantWhenDatabaseIsMissing() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        when(permissionRuleService.matchesEnabledRule(5L, null, null)).thenReturn(false);

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                null,
                null,
                List.of("DELETE FROM orders WHERE id = 1"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(List.of(
                        option(PermissionScopeType.CONVERSATION, PermissionGrantPreset.CONNECTION_ALL_DATABASES),
                        option(PermissionScopeType.USER, PermissionGrantPreset.CONNECTION_ALL_DATABASES)
                ),
                result.getConfirmation().getAvailableGrantOptions());
    }

    @Test
    void executeNonSelectSql_executesAfterOneTimeApproval() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        when(permissionRuleService.matchesEnabledRule(5L, "sales", "public")).thenReturn(false);
        writeExecutionApprovalStore.approve(7L, 42L, new DbContext(5L, "sales", "public"), "DELETE FROM orders WHERE id = 1");
        when(sqlExecutionService.executeBatchSql(any(), any())).thenReturn(List.of(
                ExecuteSqlResponse.builder()
                        .success(true)
                        .affectedRows(1)
                        .build()
        ));

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("DELETE FROM orders WHERE id = 1"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(ExecuteNonSelectToolResult.Status.EXECUTED, result.getStatus());
        assertFalse(result.isRequiresConfirmation());
        assertFalse(result.isRuleMatched());
        assertNotNull(result.getExecution());
        assertTrue(result.getExecution().isSuccess());
        assertEquals(1, result.getExecution().getResults().size());
        assertEquals(1, result.getExecution().getResults().get(0).getAffectedRows());
        assertEquals("Write SQL executed after explicit user approval.", result.getMessage());

        assertFalse(writeExecutionApprovalStore.consumeApproved(new DbContext(5L, "sales", "public"), "DELETE FROM orders WHERE id = 1"));
        verify(sqlExecutionService).executeBatchSql(eq(new DbContext(5L, "sales", "public")), eq(List.of("DELETE FROM orders WHERE id = 1")));
    }

    @Test
    void executeNonSelectSql_requiresConfirmationWhenApprovalDoesNotMatchExactScope() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
        when(permissionRuleService.matchesEnabledRule(5L, "sales", "public")).thenReturn(false);
        writeExecutionApprovalStore.approve(7L, 42L, new DbContext(5L, "sales", "archive"), "DELETE FROM orders WHERE id = 1");

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("DELETE FROM orders WHERE id = 1"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(ExecuteNonSelectToolResult.Status.REQUIRES_CONFIRMATION, result.getStatus());
        assertTrue(result.isRequiresConfirmation());
        assertNull(result.getExecution());
    }

    @Test
    void executeNonSelectSql_executesImmediatelyWhenPermissionMatches() {
        when(permissionRuleService.matchesEnabledRule(5L, "sales", "public")).thenReturn(true);
        when(sqlExecutionService.executeBatchSql(any(), any())).thenReturn(List.of(
                ExecuteSqlResponse.builder()
                        .success(true)
                        .affectedRows(2)
                        .build()
        ));

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("UPDATE orders SET status = 'done'"),
                InvocationParameters.from(Map.of())
        );

        assertEquals(ExecuteNonSelectToolResult.Status.EXECUTED, result.getStatus());
        assertTrue(result.isRuleMatched());
        assertEquals("Write SQL executed using a default-allow permission.", result.getMessage());
        assertEquals(2, result.getExecution().getResults().get(0).getAffectedRows());
    }

    @Test
    void executeNonSelectSql_rewritesDatabaseFailureIntoGuidanceMessage() {
        when(permissionRuleService.matchesEnabledRule(5L, "sales", "public")).thenReturn(true);
        when(sqlExecutionService.executeBatchSql(any(), any())).thenReturn(List.of(
                ExecuteSqlResponse.builder()
                        .success(false)
                        .errorMessage("duplicate key value violates unique constraint")
                        .build()
        ));

        ExecuteNonSelectToolResult result = tool.executeNonSelectSql(
                5L,
                "sales",
                "public",
                List.of("INSERT INTO orders(id) VALUES (1)"),
                InvocationParameters.from(Map.of())
        );

        AgentSqlResult execution = result.getExecution();
        assertNotNull(execution);
        assertTrue(execution.isSuccess());
        assertTrue(execution.getResults() != null && execution.getResults().size() == 1);
        AgentSqlResult failedStatement = execution.getResults().get(0);
        assertFalse(failedStatement.isSuccess());
        assertTrue(failedStatement.getError().contains("Write SQL failed for connectionId=5, database=sales, schema=public"));
        assertTrue(failedStatement.getError().contains("INSERT INTO orders(id) VALUES (1)"));
        assertTrue(failedStatement.getError().contains("duplicate key value violates unique constraint"));
        assertTrue(failedStatement.getError().contains("Do not retry a write blindly"));
    }

    private static WriteExecutionGrantOption option(PermissionScopeType scopeType, PermissionGrantPreset grantPreset) {
        return WriteExecutionGrantOption.builder()
                .scopeType(scopeType)
                .grantPreset(grantPreset)
                .build();
    }
}
