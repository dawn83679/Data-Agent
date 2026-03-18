package edu.zsc.ai.agent.subagent.explorer;

import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplorerResponseParserTest {

    @Test
    void parse_normalizesAndSortsRelevanceScores() {
        String response = """
                {
                  "summaryText": "found objects",
                  "objects": [
                    {
                      "objectName": "audit_log",
                      "objectType": "TABLE",
                      "objectDdl": "",
                      "relevanceScore": -10
                    },
                    {
                      "objectName": "orders",
                      "objectType": "TABLE",
                      "objectDdl": "",
                      "relevanceScore": 120
                    },
                    {
                      "objectName": "customers",
                      "objectType": "TABLE",
                      "objectDdl": ""
                    }
                  ],
                  "rawResponse": "done"
                }
                """;

        SchemaSummary summary = ExplorerResponseParser.parse(response);

        assertEquals(3, summary.getObjects().size());
        assertEquals("orders", summary.getObjects().get(0).getObjectName());
        assertEquals(100, summary.getObjects().get(0).getRelevanceScore());
        assertEquals("customers", summary.getObjects().get(1).getObjectName());
        assertEquals(50, summary.getObjects().get(1).getRelevanceScore());
        assertEquals("audit_log", summary.getObjects().get(2).getObjectName());
        assertEquals(0, summary.getObjects().get(2).getRelevanceScore());
    }
}
