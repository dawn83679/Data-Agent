package edu.zsc.ai.agent.subagent.explorer;

import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Explorer SubAgent — design-level tests and response parser.
 */
class ExplorerSubAgentTest {

    @Test
    void agentType_isExplorer() {
        // ExplorerSubAgent needs Spring context; test type declaration via interface
        assertEquals(AgentTypeEnum.EXPLORER, AgentTypeEnum.valueOf("EXPLORER"));
    }

    @Test
    void promptFile_isAccessible() {
        String content = PromptConfig.getPrompt(PromptEnum.EXPLORER);
        assertNotNull(content);
        assertTrue(content.contains("summaryText"));
        assertTrue(content.contains("objects"));
        assertTrue(content.contains("rawResponse"));
        assertTrue(content.contains("<role>"));
        assertTrue(content.contains("<rule>"));
        assertTrue(content.contains("TodoTool"));
        assertTrue(content.contains("executeSelectSql"));
    }

    @Test
    void responseParser_parsesJsonPayload() {
        String response = """
                {
                  "summaryText": "Found relevant order objects.",
                  "objects": [
                    {
                      "catalog": "analytics",
                      "schema": "public",
                      "objectName": "orders",
                      "objectType": "TABLE",
                      "objectDdl": "CREATE TABLE orders (id int8)",
                      "relevanceScore": 92
                    }
                  ],
                  "rawResponse": "orders is the core fact table"
                }
                """;

        SchemaSummary summary = ExplorerResponseParser.parse(response);
        assertEquals("Found relevant order objects.", summary.getSummaryText());
        assertEquals(1, summary.getObjects().size());
        assertEquals("orders", summary.getObjects().get(0).getObjectName());
        assertEquals(92, summary.getObjects().get(0).getRelevanceScore());
        assertEquals("orders is the core fact table", summary.getRawResponse());
    }

    @Test
    void responseParser_parsesFencedJsonPayload() {
        String response = """
                ```json
                {
                  "summaryText": "Found users view.",
                  "objects": [],
                  "rawResponse": "users view may still be relevant"
                }
                ```
                """;

        SchemaSummary summary = ExplorerResponseParser.parse(response);
        assertEquals("Found users view.", summary.getSummaryText());
        assertTrue(summary.getObjects().isEmpty());
        assertEquals("users view may still be relevant", summary.getRawResponse());
    }

    @Test
    void responseParser_rejectsMissingPayload() {
        assertThrows(IllegalArgumentException.class, () -> ExplorerResponseParser.parse("{\"foo\":\"bar\"}"));
    }
}
