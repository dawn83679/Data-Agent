package edu.zsc.ai.agent.subagent.explorer;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExplorerToolResultCollector.
 */
class ExplorerToolResultCollectorTest {

    private ToolExecutionRequest getObjectDetailRequest() {
        return ToolExecutionRequest.builder()
                .id("tool-1")
                .name("getObjectDetail")
                .arguments(JsonUtil.object2json(java.util.Map.of(
                        "objects", List.of(
                                new ObjectQueryItem("TABLE", "users", 1L, "analytics", "public"),
                                new ObjectQueryItem("TABLE", "orders", 1L, "analytics", "public")
                        )
                )))
                .build();
    }

    @Test
    void buildAndClear_emptyCollector_returnsEmptyObjects() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        SchemaSummary result = collector.buildAndClear("raw text");
        assertNotNull(result);
        assertEquals("raw text", result.getRawResponse());
        assertTrue(result.getObjects().isEmpty());
    }

    @Test
    void buildAndClear_nullRawResponse_usesEmptyString() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        SchemaSummary result = collector.buildAndClear(null);
        assertNotNull(result);
        assertEquals("", result.getRawResponse());
    }

    @Test
    void onToolExecuted_getObjectDetail_collectsObjects() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        List<NamedObjectDetail> details = List.of(
                new NamedObjectDetail("users", "TABLE", 1L, "analytics", "public", true, null,
                        "CREATE TABLE users...", 100L, List.of()),
                new NamedObjectDetail("orders", "TABLE", 1L, "analytics", "public", true, null,
                        "CREATE TABLE orders...", 50L, List.of())
        );
        AgentToolResult agentResult = AgentToolResult.success(details);
        collector.onToolExecuted(getObjectDetailRequest(), agentResult);

        SchemaSummary result = collector.buildAndClear("Explored 2 objects.");
        assertNotNull(result);
        assertEquals(2, result.getObjects().size());
        assertEquals("users", result.getObjects().get(0).getObjectName());
        assertEquals("orders", result.getObjects().get(1).getObjectName());
        assertEquals("analytics", result.getObjects().get(0).getCatalog());
        assertEquals("public", result.getObjects().get(0).getSchema());
        assertEquals(50, result.getObjects().get(0).getRelevanceScore());
    }

    @Test
    void onToolExecuted_otherTool_ignored() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        collector.onToolExecuted(
                ToolExecutionRequest.builder().id("tool-2").name("searchObjects").arguments("{}").build(),
                AgentToolResult.success(List.of())
        );
        SchemaSummary result = collector.buildAndClear("text");
        assertTrue(result.getObjects().isEmpty());
    }

    @Test
    void onToolExecuted_jsonString_parsesCorrectly() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        List<NamedObjectDetail> details = List.of(
                new NamedObjectDetail("products", "TABLE", 1L, "analytics", "public", true, null,
                        "CREATE TABLE products...", 200L, List.of())
        );
        AgentToolResult agentResult = AgentToolResult.success(details);
        String json = JsonUtil.object2json(agentResult);
        collector.onToolExecuted(
                ToolExecutionRequest.builder()
                        .id("tool-3")
                        .name("getObjectDetail")
                        .arguments(JsonUtil.object2json(java.util.Map.of(
                                "objects", List.of(new ObjectQueryItem("TABLE", "products", 1L, "analytics", "public"))
                        )))
                        .build(),
                json
        );

        SchemaSummary result = collector.buildAndClear("done");
        assertEquals(1, result.getObjects().size());
        assertEquals("products", result.getObjects().get(0).getObjectName());
    }

    @Test
    void buildAndClear_clearsCollector() {
        ExplorerToolResultCollector collector = new ExplorerToolResultCollector();
        collector.onToolExecuted(ToolExecutionRequest.builder()
                        .id("tool-4")
                        .name("getObjectDetail")
                        .arguments(JsonUtil.object2json(java.util.Map.of(
                                "objects", List.of(new ObjectQueryItem("TABLE", "t1", 1L, "analytics", "public"))
                        )))
                        .build(),
                AgentToolResult.success(List.of(
                        new NamedObjectDetail("t1", "TABLE", 1L, "analytics", "public", true, null,
                                "ddl", 1L, List.of()))));
        SchemaSummary first = collector.buildAndClear("r1");
        assertEquals(1, first.getObjects().size());

        SchemaSummary second = collector.buildAndClear("r2");
        assertTrue(second.getObjects().isEmpty());
        assertEquals("r2", second.getRawResponse());
    }
}
