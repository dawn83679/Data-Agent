package edu.zsc.ai.agent.tool.chart;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.guard.AgentModeGuard;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ChartTypeEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ChartTool {

    @Tool({
            "Calling this tool greatly improves how users understand data — one clear chart is far more ",
            "effective than raw tables for many questions. ",
            "Renders one chart; put key insight in description — the chart IS the final answer; do not add text afterward.",
            "",
            "When to Use: when you have query results and the user wants a visual; after executeSelectSql (or equivalent) with data ready.",
            "When NOT to Use: when the user only asked for tables or numbers; when dimension is unclear — ask with askUserQuestion first.",
            "Relation: call activateSkill('chart') before first use in the session; if dimension unspecified, askUserQuestion then render one targeted chart."
    })
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional explanation for users: chart meaning, key insight(s), and reading guide", required = false)
            String description,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] renderChart, chartType={}", chartType);
            AgentModeGuard.assertNotPlanMode(ToolNameEnum.RENDER_CHART);

            ChartTypeEnum normalizedType = ChartTypeEnum.fromValue(chartType);
            JsonNode optionNode = JsonUtil.readObjectNode(optionJson, "optionJson");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("chartType", normalizedType.name());
            result.put("option", optionJson);
            if (StringUtils.isNotBlank(description)) {
                result.put("description", description.trim());
            }

            log.info("[Tool done] renderChart, chartType={}, optionKeys={}",
                    normalizedType, optionNode.size());
            return AgentToolResult.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("[Tool invalid] renderChart, chartType={}, reason={}", chartType, e.getMessage());
            return AgentToolResult.fail("Invalid renderChart input: " + e.getMessage()
                    + ". chartType must be one of LINE/BAR/PIE/SCATTER/AREA, and optionJson must be valid ECharts JSON.");
        } catch (Exception e) {
            log.error("[Tool error] renderChart, chartType={}", chartType, e);
            return AgentToolResult.fail("Failed to render chart (chartType=" + chartType + "): " + e.getMessage()
                    + ". Verify chartType is one of LINE/BAR/PIE/SCATTER/AREA and optionJson is valid ECharts JSON.");
        }
    }
}
