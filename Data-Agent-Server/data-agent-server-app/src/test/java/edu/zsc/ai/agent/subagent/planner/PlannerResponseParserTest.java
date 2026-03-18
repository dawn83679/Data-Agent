package edu.zsc.ai.agent.subagent.planner;

import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlannerResponseParserTest {

    @Test
    void parsesStructuredPlannerJson() {
        String response = """
                {
                  "summaryText": "Aggregate revenue by day.",
                  "planSteps": [
                    {
                      "title": "Select source table",
                      "content": "Use orders as the fact table."
                    }
                  ],
                  "sqlBlocks": [
                    {
                      "title": "Final SQL",
                      "sql": "select date(created_at), sum(total) from public.orders group by 1",
                      "kind": "FINAL"
                    }
                  ],
                  "rawResponse": "Use orders and aggregate by created_at day."
                }
                """;

        SqlPlan plan = PlannerResponseParser.parse(response);

        assertEquals("Aggregate revenue by day.", plan.getSummaryText());
        assertEquals(1, plan.getPlanSteps().size());
        assertEquals(1, plan.getSqlBlocks().size());
        assertEquals(edu.zsc.ai.agent.subagent.contract.SqlPlanBlockKind.FINAL, plan.getSqlBlocks().get(0).getKind());
    }

    @Test
    void rejectsBlankPayload() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> PlannerResponseParser.parse("   ")
        );

        assertTrue(error.getMessage().contains("blank"));
    }

    @Test
    void rejectsJsonWithoutExpectedFields() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> PlannerResponseParser.parse("{\"foo\":\"bar\"}")
        );

        assertTrue(error.getMessage().contains("expected JSON schema"));
    }
}
