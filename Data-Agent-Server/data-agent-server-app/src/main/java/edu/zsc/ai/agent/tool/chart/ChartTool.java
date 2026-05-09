package edu.zsc.ai.agent.tool.chart;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
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
            "Value: renders verified data as one ECharts chart.",
            "Use When: data is ready and the user needs a chart, trend, comparison, or distribution.",
            "Preconditions: activate chart skill first; chartType must be supported; optionJson must be valid ECharts JSON.",
            "Result: the chart is the final answer for this turn.",
            "Boundary: render at most one chart per turn and emit no assistant text after this tool call."
    })
    @DisallowInPlanMode(ToolNameEnum.RENDER_CHART)
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional user-facing chart explanation.", required = false)
            String description) {
        log.info("[Tool] renderChart, chartType={}", chartType);

        ChartTypeEnum normalizedType;
        JsonNode optionNode;
        try {
            normalizedType = ChartTypeEnum.fromValue(chartType);
            optionNode = JsonUtil.readObjectNode(optionJson, "optionJson");
        } catch (IllegalArgumentException e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.RENDER_CHART,
                    "Invalid renderChart input: " + e.getMessage()
                            + ". chartType must be one of LINE/BAR/PIE/SCATTER/AREA, and optionJson must be valid ECharts JSON. "
                            + "Fix the chart input and retry; do not invent chart conclusions without a rendered chart."
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chartType", normalizedType.name());
        result.put("option", optionJson);
        if (StringUtils.isNotBlank(description)) {
            result.put("description", description.trim());
        }

        log.info("[Tool done] renderChart, chartType={}, optionKeys={}",
                normalizedType, optionNode.size());
        return AgentToolResult.success(result, ToolMessageSupport.sentence(
                "Chart rendering payload is ready.",
                "The chart is the final answer for this turn. End the turn immediately — do not emit any assistant text describing, summarizing, or commenting on the chart afterwards. Any insight or reading guide should already live inside the description parameter, not in a follow-up message."
        ));
    }
}
