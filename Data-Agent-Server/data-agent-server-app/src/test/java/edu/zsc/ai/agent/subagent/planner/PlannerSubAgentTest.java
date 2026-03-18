package edu.zsc.ai.agent.subagent.planner;

import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Planner SubAgent — design-level tests, message building, and prompt validation.
 */
class PlannerSubAgentTest {

    @Test
    void agentType_isPlanner() {
        assertEquals(AgentTypeEnum.PLANNER, AgentTypeEnum.valueOf("PLANNER"));
    }

    @Test
    void implementsSubAgentInterface() {
        assertTrue(SubAgent.class.isAssignableFrom(PlannerSubAgent.class));
    }

    @Test
    void invokeMethod_acceptsPlannerRequest() throws Exception {
        Method invokeMethod = PlannerSubAgent.class.getMethod("invoke", PlannerRequest.class);
        assertNotNull(invokeMethod);
        assertEquals(SqlPlan.class, invokeMethod.getReturnType());
    }

    // ==================== Message Building ====================

    @Nested
    class MessageBuildingTest {

        /**
         * Uses reflection to test the private buildMessage method.
         */
        private String invokeBuildMessage(PlannerRequest request) throws Exception {
            Method method = PlannerSubAgent.class.getDeclaredMethod("buildMessage", PlannerRequest.class);
            method.setAccessible(true);
            PlannerSubAgent agent = createTestableAgent();
            return (String) method.invoke(agent, request);
        }

        private PlannerSubAgent createTestableAgent() throws Exception {
            var constructor = PlannerSubAgent.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object[] args = new Object[constructor.getParameterCount()];
            return (PlannerSubAgent) constructor.newInstance(args);
        }

        @Test
        void basicMessage_containsQuestionAndSchema() throws Exception {
            SchemaSummary schema = SchemaSummary.builder()
                    .objects(List.of(
                            ExploreObject.builder()
                                    .catalog("analytics")
                                    .schema("public")
                                    .objectName("orders")
                                    .objectType("TABLE")
                                    .objectDdl("CREATE TABLE orders (id int8 NOT NULL, total numeric)")
                                    .relevanceScore(93)
                                    .build()
                    ))
                    .rawResponse("orders is the main fact table")
                    .build();

            PlannerRequest request = PlannerRequest.builder()
                    .instruction("Get total revenue")
                    .schemaSummary(schema)
                    .build();

            String message = invokeBuildMessage(request);

            assertTrue(message.contains("Get total revenue"), "Should contain instruction");
            assertTrue(message.contains("orders"), "Should contain object name");
            assertTrue(message.contains("TABLE"), "Should contain object type");
            assertTrue(message.contains("analytics"), "Should contain catalog");
            assertTrue(message.contains("CREATE TABLE orders"), "Should contain DDL");
        }

        @Test
        void instructionWithOptimizationContext_passedThrough() throws Exception {
            PlannerRequest request = PlannerRequest.builder()
                    .instruction("Optimize: SELECT * FROM orders WHERE status = 'active'. DDL: CREATE TABLE orders (id int8). Index: idx_orders_id")
                    .schemaSummary(SchemaSummary.builder().objects(List.of()).build())
                    .build();

            String message = invokeBuildMessage(request);

            assertTrue(message.contains("SELECT * FROM orders"), "Instruction should contain optimization SQL");
            assertTrue(message.contains("idx_orders_id"), "Instruction should contain index info");
        }

        @Test
        void messageWithNullSchema_handleGracefully() throws Exception {
            PlannerRequest request = PlannerRequest.builder()
                    .instruction("Some question")
                    .schemaSummary(null)
                    .build();

            String message = invokeBuildMessage(request);

            assertTrue(message.contains("no schema information"), "Should handle null schema");
        }

        @Test
        void messageWithRawResponse_serializesExplorerReasoning() throws Exception {
            SchemaSummary schema = SchemaSummary.builder()
                    .objects(List.of())
                    .rawResponse("order_items joins to orders via order_id")
                    .build();

            PlannerRequest request = PlannerRequest.builder()
                    .instruction("Join query")
                    .schemaSummary(schema)
                    .build();

            String message = invokeBuildMessage(request);

            assertTrue(message.contains("Explorer Raw Response"), "Should contain raw response section");
            assertTrue(message.contains("order_items joins to orders"), "Should contain raw response content");
        }

        @Test
        void messageWithRelevanceScore_serializesObjectScore() throws Exception {
            SchemaSummary schema = SchemaSummary.builder()
                    .objects(List.of(
                            ExploreObject.builder()
                                    .objectName("users")
                                    .objectType("TABLE")
                                    .relevanceScore(64)
                                    .build()
                    ))
                    .build();

            PlannerRequest request = PlannerRequest.builder()
                    .instruction("Find users")
                    .schemaSummary(schema)
                    .build();

            String message = invokeBuildMessage(request);

            assertTrue(message.contains("relevanceScore=64"), "Should contain relevance score marker");
        }
    }

    // ==================== Prompt Validation ====================

    @Nested
    class PromptValidationTest {

        @Test
        void promptFile_isAccessible() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertNotNull(content);
            assertTrue(content.contains("<identity>"));
        }

        @Test
        void promptFile_identityUnderThreeLines() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            int start = content.indexOf("<identity>");
            int end = content.indexOf("</identity>");
            String identity = content.substring(start, end);
            long lines = identity.lines().filter(l -> !l.isBlank() && !l.contains("<")).count();
            assertTrue(lines <= 3, "Identity should be ≤3 lines, got " + lines);
        }

        @Test
        void promptFile_usesPitfallsFormat() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("<dql-pitfalls"), "Should have DQL pitfalls");
            assertTrue(content.contains("<dml-pitfalls"), "Should have DML pitfalls");
            assertTrue(content.contains("<ddl-pitfalls"), "Should have DDL pitfalls");
        }

        @Test
        void promptFile_hasThreeRules() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("1."));
            assertTrue(content.contains("2."));
            assertTrue(content.contains("3."));
        }

        @Test
        void promptFile_hasFourStageWorkflow() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("阶段 1"), "Should have stage 1");
            assertTrue(content.contains("阶段 2"), "Should have stage 2");
            assertTrue(content.contains("阶段 3"), "Should have stage 3");
            assertTrue(content.contains("阶段 4"), "Should have stage 4");
        }

        @Test
        void promptFile_mentionsActivateSkill() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("activateSkill"), "Should mention activateSkill");
            assertTrue(content.contains("sql-optimization"), "Should mention sql-optimization skill");
        }

        @Test
        void promptFile_mentionsGetObjectDetail() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("getObjectDetail"), "Should mention getObjectDetail");
        }

        @Test
        void promptFile_requiresStructuredJsonOutput() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertTrue(content.contains("summaryText"), "Should mention summaryText output");
            assertTrue(content.contains("planSteps"), "Should mention planSteps output");
            assertTrue(content.contains("sqlBlocks"), "Should mention sqlBlocks output");
            assertTrue(content.contains("rawResponse"), "Should mention rawResponse output");
        }

        @Test
        void promptFile_noToolMasteryBlock() {
            String content = PromptConfig.getPrompt(PromptEnum.PLANNER);
            assertFalse(content.contains("<tool-mastery>"), "Planner should NOT have tool-mastery");
        }
    }
}
