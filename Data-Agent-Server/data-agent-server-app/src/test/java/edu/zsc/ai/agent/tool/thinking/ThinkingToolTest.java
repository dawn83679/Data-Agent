package edu.zsc.ai.agent.tool.thinking;

import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ThinkingStageEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThinkingToolTest {

    private final ThinkingTool tool = new ThinkingTool();

    @Test
    void thinking_returnsStructuredVisibleDecision() {
        AgentToolResult result = tool.thinking(
                ThinkingStageEnum.SQL,
                "已确认订单表和订阅表可以通过 order_id 关联。",
                List.of("connectionId=3", "database=enterprise_gateway_dev", "order_item.order_id 存在"),
                List.of("getObjectDetail 返回 order_item 与 subscription_item 字段结构"),
                List.of("订阅状态枚举仍需确认"),
                List.of("直接更新前需要确认影响范围"),
                "先用只读 SQL 查询目标订单和订阅记录。",
                "记录 SQL 前的数据判断",
                null
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("继续执行 nextAction"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result.getResult();
        assertEquals("SQL", payload.get("stage"));
        assertEquals("已确认订单表和订阅表可以通过 order_id 关联。", payload.get("summary"));
        assertEquals(List.of("订阅状态枚举仍需确认"), payload.get("openQuestions"));
        assertEquals("先用只读 SQL 查询目标订单和订阅记录。", payload.get("nextAction"));
        assertEquals("记录 SQL 前的数据判断", payload.get("description"));
    }

    @Test
    void thinking_normalizesMissingOptionalListsAndStage() {
        AgentToolResult result = tool.thinking(
                null,
                "当前数据范围仍不完整。",
                null,
                null,
                null,
                null,
                "询问用户确认数据库。",
                null,
                null
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) result.getResult();
        assertEquals("OTHER", payload.get("stage"));
        assertEquals(List.of(), payload.get("confirmedFacts"));
        assertEquals(List.of(), payload.get("evidence"));
        assertEquals(List.of(), payload.get("openQuestions"));
        assertEquals(List.of(), payload.get("risks"));
        assertEquals("", payload.get("description"));
    }
}
