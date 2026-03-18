package edu.zsc.ai.agent.subagent.contract;

import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubAgentContractTest {

    // ==================== ExplorerRequest ====================

    @Nested
    class ExplorerRequestTest {

        @Test
        void requiredFieldsOnly() {
            ExplorerRequest req = ExplorerRequest.builder()
                    .instruction("explore order tables")
                    .connectionIds(List.of(1L))
                    .build();

            assertEquals("explore order tables", req.getInstruction());
            assertEquals(List.of(1L), req.getConnectionIds());
            assertNull(req.getContext());
        }

        @Test
        void allFields() {
            ExplorerRequest req = ExplorerRequest.builder()
                    .instruction("retry finding tables")
                    .connectionIds(List.of(1L, 2L))
                    .context("previous error: column amount not found")
                    .build();

            assertNotNull(req.getContext());
            assertEquals(2, req.getConnectionIds().size());
        }

        @Test
        void serializable() {
            ExplorerRequest req = ExplorerRequest.builder()
                    .instruction("test")
                    .connectionIds(List.of(1L))
                    .build();

            String json = JsonUtil.object2json(req);
            assertTrue(json.contains("instruction"));
            assertTrue(json.contains("connectionIds"));
        }
    }

    // ==================== SchemaSummary ====================

    @Nested
    class SchemaSummaryTest {

        @Test
        void buildWithObjectsAndRawResponse() {
            ExploreObject ordersObject = ExploreObject.builder()
                    .catalog("analytics")
                    .schema("public")
                    .objectName("orders")
                    .objectType("TABLE")
                    .objectDdl("CREATE TABLE orders (id int8, customer_id int8, total numeric)")
                    .relevanceScore(92)
                    .build();

            SchemaSummary summary = SchemaSummary.builder()
                    .objects(List.of(ordersObject))
                    .rawResponse("orders is a core object for revenue analysis")
                    .build();

            assertEquals(1, summary.getObjects().size());
            assertEquals("orders", summary.getObjects().get(0).getObjectName());
            assertEquals("TABLE", summary.getObjects().get(0).getObjectType());
            assertEquals("orders is a core object for revenue analysis", summary.getRawResponse());
        }

        @Test
        void serializable() {
            SchemaSummary summary = SchemaSummary.builder()
                    .objects(List.of(
                            ExploreObject.builder()
                                    .catalog("analytics")
                                    .schema("public")
                                    .objectName("users")
                                    .objectType("TABLE")
                                    .objectDdl("CREATE TABLE users (id int8)")
                                    .relevanceScore(88)
                                    .build()
                    ))
                    .rawResponse("users object found")
                    .build();

            String json = JsonUtil.object2json(summary);
            assertTrue(json.contains("users"));
            assertTrue(json.contains("relevanceScore"));
            assertTrue(json.contains("rawResponse"));
        }
    }

    // ==================== PlannerRequest ====================

    @Nested
    class PlannerRequestTest {

        @Test
        void requiredFieldsOnly() {
            PlannerRequest req = PlannerRequest.builder()
                    .instruction("generate monthly revenue by category")
                    .schemaSummary(SchemaSummary.builder().objects(List.of()).build())
                    .build();

            assertNotNull(req.getInstruction());
            assertNotNull(req.getSchemaSummary());
        }

        @Test
        void instructionIncludesOptimizationContext() {
            PlannerRequest req = PlannerRequest.builder()
                    .instruction("optimize: SELECT * FROM orders. DDL: CREATE TABLE orders (id int8). Index: idx_orders_id")
                    .schemaSummary(SchemaSummary.builder().objects(List.of()).build())
                    .build();

            assertTrue(req.getInstruction().contains("optimize"));
            assertTrue(req.getInstruction().contains("SELECT * FROM orders"));
        }
    }

    // ==================== SqlPlan ====================

    @Nested
    class SqlPlanTest {

        @Test
        void basicPlan() {
            SqlPlan plan = SqlPlan.builder()
                    .summaryText("Aggregate customer revenue.")
                    .planSteps(List.of(
                            SqlPlanStep.builder()
                                    .title("Join customers and orders")
                                    .content("Use customer_id to join orders to customers.")
                                    .build()
                    ))
                    .sqlBlocks(List.of(
                            SqlPlanBlock.builder()
                                    .title("Final SQL")
                                    .sql("SELECT c.name, SUM(o.total) FROM customers c JOIN orders o ON c.id = o.customer_id GROUP BY c.name")
                                    .kind(SqlPlanBlockKind.FINAL)
                                    .build()
                    ))
                    .rawResponse("Join customers and orders, then aggregate by customer name.")
                    .build();

            assertEquals("Aggregate customer revenue.", plan.getSummaryText());
            assertEquals(1, plan.getPlanSteps().size());
            assertEquals(1, plan.getSqlBlocks().size());
            assertNotNull(plan.getRawResponse());
        }

        @Test
        void serializable() {
            SqlPlan plan = SqlPlan.builder()
                    .summaryText("Select one.")
                    .sqlBlocks(List.of(
                            SqlPlanBlock.builder()
                                    .title("Final SQL")
                                    .sql("SELECT 1")
                                    .kind(SqlPlanBlockKind.FINAL)
                                    .build()
                    ))
                    .rawResponse("Use SELECT 1.")
                    .build();

            String json = JsonUtil.object2json(plan);
            assertTrue(json.contains("SELECT 1"));
            assertTrue(json.contains("summaryText"));
            assertTrue(json.contains("sqlBlocks"));
        }
    }
}
