package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.common.enums.ai.AgentTypeEnum;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SubAgent interface contract.
 * Verifies the interface design supports different return types
 * and is properly typed for Explorer and Planner use cases.
 */
class SubAgentInterfaceTest {

    // Simulate SchemaSummary and SqlPlan as simple test DTOs
    record TestSchemaSummary(String tableName) {}
    record TestSqlPlan(String sql) {}

    // Concrete test implementations to verify the interface works with different return types

    static class FakeExplorer implements SubAgent<SubAgentRequest, TestSchemaSummary> {
        @Override
        public AgentTypeEnum getAgentType() {
            return AgentTypeEnum.EXPLORER;
        }

        @Override
        public TestSchemaSummary invoke(SubAgentRequest request) {
            return new TestSchemaSummary("orders");
        }
    }

    record TestPlannerRequest(String instruction, String schemaSummary) {}

    static class FakePlanner implements SubAgent<TestPlannerRequest, TestSqlPlan> {
        @Override
        public AgentTypeEnum getAgentType() {
            return AgentTypeEnum.PLANNER;
        }

        @Override
        public TestSqlPlan invoke(TestPlannerRequest request) {
            return new TestSqlPlan("SELECT * FROM orders");
        }
    }

    @Test
    void explorerSubAgent_returnsCorrectType() {
        SubAgent<SubAgentRequest, TestSchemaSummary> explorer = new FakeExplorer();

        assertEquals(AgentTypeEnum.EXPLORER, explorer.getAgentType());

        SubAgentRequest request = new SubAgentRequest(
                "show me the orders table",
                List.of(1L),
                null
        );
        TestSchemaSummary result = explorer.invoke(request);

        assertNotNull(result);
        assertEquals("orders", result.tableName());
    }

    @Test
    void plannerSubAgent_returnsCorrectType() {
        SubAgent<TestPlannerRequest, TestSqlPlan> planner = new FakePlanner();

        assertEquals(AgentTypeEnum.PLANNER, planner.getAgentType());

        TestPlannerRequest request = new TestPlannerRequest(
                "get all orders",
                "tables: [orders]"
        );
        TestSqlPlan result = planner.invoke(request);

        assertNotNull(result);
        assertEquals("SELECT * FROM orders", result.sql());
    }

    @Test
    void subAgentRequest_carriesAllFields() {
        SubAgentRequest request = new SubAgentRequest(
                "explore tables",
                List.of(1L, 2L),
                "previous error: column not found"
        );

        assertEquals("explore tables", request.instruction());
        assertEquals(List.of(1L, 2L), request.connectionIds());
        assertEquals("previous error: column not found", request.context());
    }

    @Test
    void subAgentRequest_optionalContextCanBeNull() {
        SubAgentRequest request = new SubAgentRequest(
                "explore tables",
                List.of(1L),
                null
        );

        assertNull(request.context());
        assertEquals(1, request.connectionIds().size());
    }
}
