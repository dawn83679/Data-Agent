package edu.zsc.ai.agent.tool.chart;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.guard.AgentModeGuard;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.enums.ai.ChartTypeEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ChartTool {

    @Tool({
            "The best way to deliver data answers — a focused, well-chosen chart communicates ",
            "insights far more effectively than raw tables. Users strongly prefer visual results ",
            "when the chart is targeted and relevant to what they asked.",
            "",
            "Quality over quantity: render ONE chart that directly answers the user's question. ",
            "If the user hasn't specified what dimension to visualize, ask them first with ",
            "askUserQuestion — a targeted chart is 10x more valuable than multiple generic ones. ",
            "Put your key insight in the description parameter — the chart IS the final answer, ",
            "do not repeat data or add commentary in text afterward."
    })
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional explanation for users: chart meaning, key insight(s), and reading guide", required = false)
            String description,
            InvocationParameters parameters) {
        log.info("[Tool] renderChart, chartType={}", chartType);
        try {
            AgentModeGuard.assertNotPlanMode(parameters, ToolNameEnum.RENDER_CHART);
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }

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
