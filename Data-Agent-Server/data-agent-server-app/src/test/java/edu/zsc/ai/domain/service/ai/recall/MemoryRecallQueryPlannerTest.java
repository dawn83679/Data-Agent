package edu.zsc.ai.domain.service.ai.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.constant.MemoryRecallPlanningConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;

class MemoryRecallQueryPlannerTest {

    private final MemoryRecallQueryPlanner planner = new MemoryRecallQueryPlanner(new MemoryRecallPlanningRules());

    @Test
    void plan_defaultsToAllScopesWhenScopeIsBlank() {
        List<MemoryRecallQuery> queries = planner.plan(MemoryRecallContext.builder()
                .conversationId(7L)
                .queryText("find memory")
                .memoryType("WORKFLOW_CONSTRAINT")
                .subType("IMPLEMENTATION_CONSTRAINT")
                .recallMode(MemoryRecallMode.PROMPT)
                .build());

        assertEquals(List.of(
                        MemoryScopeEnum.CONVERSATION.getCode(),
                        MemoryScopeEnum.WORKSPACE.getCode()),
                queries.stream().map(MemoryRecallQuery::targetScope).toList());
        assertEquals(MemoryRecallQueryStrategy.HYBRID, queries.get(0).queryStrategy());
        assertEquals(MemoryRecallQueryStrategy.BROWSE, queries.get(1).queryStrategy());
        assertEquals(MemoryRecallPlanningConstant.REASON_SUBTYPE_REORDERED_TO_CONVERSATION, queries.get(0).planningReason());
        assertEquals(MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_WORKFLOW_CONSTRAINT_DEFAULT, queries.get(1).planningReason());
    }

    @Test
    void plan_usesExplicitScopeOnly() {
        List<MemoryRecallQuery> queries = planner.plan(MemoryRecallContext.builder()
                .conversationId(7L)
                .queryText("find preference")
                .scope(MemoryScopeEnum.USER.getCode())
                .recallMode(MemoryRecallMode.PROMPT)
                .build());

        assertEquals(1, queries.size());
        assertEquals(MemoryScopeEnum.USER.getCode(), queries.get(0).targetScope());
        assertTrue(queries.get(0).queryName().startsWith(MemoryRecallPlanningConstant.QUERY_NAME_PREFIX + ":user"));
        assertEquals(MemoryRecallPlanningConstant.REASON_EXPLICIT_SCOPE, queries.get(0).planningReason());
    }

    @Test
    void plan_rejectsInvalidScope() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> planner.plan(
                MemoryRecallContext.builder()
                        .conversationId(7L)
                        .queryText("find memory")
                        .scope("INVALID")
                        .build()));

        assertEquals("Unsupported scope 'INVALID'. Valid values: CONVERSATION, WORKSPACE, USER", exception.getMessage());
    }

    @Test
    void plan_prefersUserScopeForPreferenceSubType() {
        List<MemoryRecallQuery> queries = planner.plan(MemoryRecallContext.builder()
                .conversationId(7L)
                .queryText("find output preference")
                .memoryType("PREFERENCE")
                .subType("OUTPUT_FORMAT")
                .recallMode(MemoryRecallMode.PROMPT)
                .build());

        assertEquals(List.of(MemoryScopeEnum.USER.getCode(), MemoryScopeEnum.CONVERSATION.getCode()),
                queries.stream().map(MemoryRecallQuery::targetScope).toList());
        assertEquals(MemoryRecallQueryStrategy.SEMANTIC, queries.get(0).queryStrategy());
        assertEquals(MemoryRecallQueryStrategy.BROWSE, queries.get(1).queryStrategy());
        assertEquals(0, queries.get(0).priority());
        assertEquals(1, queries.get(1).priority());
        assertEquals(MemoryRecallPlanningConstant.REASON_SUBTYPE_REORDERED_TO_USER, queries.get(0).planningReason());
        assertEquals(MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_PREFERENCE_DEFAULT, queries.get(1).planningReason());
    }

    @Test
    void plan_usesFallbackRuleWhenMemoryTypeIsMissing() {
        List<MemoryRecallQuery> queries = planner.plan(MemoryRecallContext.builder()
                .conversationId(7L)
                .queryText("find anything")
                .recallMode(MemoryRecallMode.PROMPT)
                .build());

        assertEquals(List.of(
                        MemoryScopeEnum.CONVERSATION.getCode(),
                        MemoryScopeEnum.WORKSPACE.getCode(),
                        MemoryScopeEnum.USER.getCode()),
                queries.stream().map(MemoryRecallQuery::targetScope).toList());
        assertEquals(MemoryRecallPlanningConstant.REASON_FALLBACK_DEFAULT_SCOPE_PLAN, queries.get(0).planningReason());
        assertEquals(MemoryRecallQueryStrategy.HYBRID, queries.get(0).queryStrategy());
        assertEquals(MemoryRecallQueryStrategy.BROWSE, queries.get(2).queryStrategy());
    }
}
