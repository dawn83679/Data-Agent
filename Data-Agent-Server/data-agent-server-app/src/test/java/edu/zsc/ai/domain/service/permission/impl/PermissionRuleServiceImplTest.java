package edu.zsc.ai.domain.service.permission.impl;

import edu.zsc.ai.agent.tool.sql.approval.WriteExecutionApprovalStore;
import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.dto.response.permission.PermissionRuleResponse;
import edu.zsc.ai.domain.model.entity.ai.AiPermissionRule;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRuleServiceImplTest {

    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final DbConnectionService dbConnectionService = mock(DbConnectionService.class);
    private final WriteExecutionApprovalStore approvalStore = new WriteExecutionApprovalStore();

    private InMemoryPermissionRuleService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryPermissionRuleService(aiConversationService, dbConnectionService, approvalStore);
        when(dbConnectionService.getConnectionById(anyLong())).thenAnswer(invocation -> connection(invocation.getArgument(0)));
        when(dbConnectionService.getAllConnections()).thenReturn(List.of(connection(5L), connection(7L)));
        doNothing().when(aiConversationService).checkAccess(anyLong(), anyLong());
        RequestContext.set(RequestContextInfo.builder()
                .userId(7L)
                .conversationId(42L)
                .build());
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void approveWriteExecution_withoutPersistedRuleStoresOnlyOneTimeApproval() {
        service.approveWriteExecution(
                42L,
                5L,
                "sales",
                "public",
                "DELETE FROM orders WHERE id = 1",
                null,
                null
        );

        assertTrue(approvalStore.consumeApproved(7L, 42L, new DbContext(5L, "sales", "public"),
                "DELETE FROM orders WHERE id = 1"));
        assertTrue(service.list().isEmpty());
    }

    @Test
    void approveWriteExecution_withDatabaseAllSchemasPersistsNormalizedRule() {
        service.approveWriteExecution(
                42L,
                5L,
                "sales",
                null,
                "DELETE FROM orders WHERE id = 1",
                PermissionScopeType.CONVERSATION,
                PermissionGrantPreset.DATABASE_ALL_SCHEMAS
        );

        List<AiPermissionRule> rules = service.list();
        assertEquals(1, rules.size());
        AiPermissionRule rule = rules.get(0);
        assertEquals(PermissionScopeType.CONVERSATION, rule.getScopeType());
        assertEquals(42L, rule.getConversationId());
        assertEquals("sales", rule.getCatalogName());
        assertNull(rule.getSchemaName());
        assertTrue(Boolean.TRUE.equals(rule.getEnabled()));
    }

    @Test
    void approveWriteExecution_withConnectionAllDatabasesPersistsWildcardRule() {
        service.approveWriteExecution(
                42L,
                5L,
                null,
                null,
                "DELETE FROM orders WHERE id = 1",
                PermissionScopeType.USER,
                PermissionGrantPreset.CONNECTION_ALL_DATABASES
        );

        List<AiPermissionRule> rules = service.list();
        assertEquals(1, rules.size());
        AiPermissionRule rule = rules.get(0);
        assertEquals(PermissionScopeType.USER, rule.getScopeType());
        assertNull(rule.getConversationId());
        assertNull(rule.getCatalogName());
        assertNull(rule.getSchemaName());
        assertTrue(Boolean.TRUE.equals(rule.getEnabled()));
    }

    @Test
    void matchesEnabledRule_matchesExactSchemaRule() {
        PermissionRuleResponse response = service.upsertForCurrentUser(
                PermissionScopeType.CONVERSATION,
                42L,
                5L,
                PermissionGrantPreset.EXACT_SCHEMA,
                "sales",
                "public",
                true
        );

        assertEquals(PermissionGrantPreset.EXACT_SCHEMA, response.getGrantPreset());
        assertTrue(service.matchesEnabledRule(5L, "sales", "public"));
        assertFalse(service.matchesEnabledRule(5L, "sales", "archive"));
    }

    @Test
    void matchesEnabledRule_matchesDatabaseAllSchemasRule() {
        service.upsertForCurrentUser(
                PermissionScopeType.USER,
                null,
                5L,
                PermissionGrantPreset.DATABASE_ALL_SCHEMAS,
                "sales",
                null,
                true
        );

        assertTrue(service.matchesEnabledRule(5L, "sales", "public"));
        assertTrue(service.matchesEnabledRule(5L, "sales", "archive"));
        assertFalse(service.matchesEnabledRule(5L, "analytics", "public"));
    }

    @Test
    void matchesEnabledRule_matchesConnectionAllDatabasesRule() {
        service.upsertForCurrentUser(
                PermissionScopeType.USER,
                null,
                5L,
                PermissionGrantPreset.CONNECTION_ALL_DATABASES,
                null,
                null,
                true
        );

        assertTrue(service.matchesEnabledRule(5L, "sales", "public"));
        assertTrue(service.matchesEnabledRule(5L, "analytics", "reporting"));
        assertFalse(service.matchesEnabledRule(7L, "sales", "public"));
    }

    @Test
    void matchesEnabledRule_prefersMoreSpecificUserRuleOverBroaderConversationRule() {
        service.upsertForCurrentUser(
                PermissionScopeType.CONVERSATION,
                42L,
                5L,
                PermissionGrantPreset.CONNECTION_ALL_DATABASES,
                null,
                null,
                false
        );
        service.upsertForCurrentUser(
                PermissionScopeType.USER,
                null,
                5L,
                PermissionGrantPreset.EXACT_SCHEMA,
                "sales",
                "public",
                true
        );

        assertTrue(service.matchesEnabledRule(5L, "sales", "public"));
        assertFalse(service.matchesEnabledRule(5L, "analytics", "public"));
    }

    @Test
    void matchesEnabledRule_prefersConversationRuleWhenSpecificityIsSame() {
        service.upsertForCurrentUser(
                PermissionScopeType.USER,
                null,
                5L,
                PermissionGrantPreset.EXACT_SCHEMA,
                "sales",
                "public",
                false
        );
        service.upsertForCurrentUser(
                PermissionScopeType.CONVERSATION,
                42L,
                5L,
                PermissionGrantPreset.EXACT_SCHEMA,
                "sales",
                "public",
                true
        );

        assertTrue(service.matchesEnabledRule(5L, "sales", "public"));
    }

    @Test
    void listForCurrentUser_returnsGrantPresetForConversationAndUserRules() {
        service.upsertForCurrentUser(
                PermissionScopeType.USER,
                null,
                5L,
                PermissionGrantPreset.CONNECTION_ALL_DATABASES,
                null,
                null,
                true
        );
        service.upsertForCurrentUser(
                PermissionScopeType.CONVERSATION,
                42L,
                5L,
                PermissionGrantPreset.DATABASE_ALL_SCHEMAS,
                "sales",
                null,
                false
        );

        List<PermissionRuleResponse> responses = service.listForCurrentUser(42L);

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(item -> item.getGrantPreset() == PermissionGrantPreset.CONNECTION_ALL_DATABASES));
        assertTrue(responses.stream().anyMatch(item -> item.getGrantPreset() == PermissionGrantPreset.DATABASE_ALL_SCHEMAS));
    }

    private static ConnectionResponse connection(Long id) {
        return ConnectionResponse.builder()
                .id(id)
                .name("conn-" + id)
                .dbType("postgresql")
                .build();
    }

    private static final class InMemoryPermissionRuleService extends PermissionRuleServiceImpl {

        private final List<AiPermissionRule> store = new ArrayList<>();
        private long nextId = 1L;

        private InMemoryPermissionRuleService(AiConversationService aiConversationService,
                                              DbConnectionService dbConnectionService,
                                              WriteExecutionApprovalStore approvalStore) {
            super(aiConversationService, dbConnectionService, approvalStore);
        }

        @Override
        public List<AiPermissionRule> list() {
            return new ArrayList<>(store);
        }

        @Override
        public boolean save(AiPermissionRule entity) {
            if (entity.getId() == null) {
                entity.setId(nextId++);
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            store.add(entity);
            return true;
        }

        @Override
        public boolean updateById(AiPermissionRule entity) {
            return true;
        }

        @Override
        public AiPermissionRule getById(Serializable id) {
            return store.stream()
                    .filter(entity -> entity.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean removeById(Serializable id) {
            return store.removeIf(entity -> entity.getId().equals(id));
        }

        @Override
        public boolean removeByIds(Collection<?> list) {
            return store.removeIf(entity -> list.contains(entity.getId()));
        }
    }
}
