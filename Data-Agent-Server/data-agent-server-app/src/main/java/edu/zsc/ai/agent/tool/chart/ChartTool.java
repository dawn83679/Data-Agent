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
            "Value: turns verified query results into a visual answer that users can understand faster than raw rows.",
            "Use When: call when the data is already available and the user wants a chart, trend, comparison, or distribution.",
            "One Chart Per Turn (CRITICAL): render at most ONE chart per user turn. Pick the single most informative view of the data and commit to it. Do NOT call renderChart multiple times in the same turn to show the same data as line + bar + pie, do NOT produce a 'dashboard' of several charts at once, and do NOT split one logical chart into several smaller charts. If the user genuinely needs another angle, ask them which one — or wait for them to ask.",
            "No Trailing Explanation (CRITICAL): the rendered chart IS the answer. After renderChart returns, end the turn immediately — do NOT emit any additional assistant text describing what the chart shows, restating the data, summarizing trends, or 'as you can see in the chart...' commentary. If you have a key insight or reading guide for the user, put it INSIDE the renderChart description parameter, not in a follow-up message. The only acceptable post-chart text is none.",
            "Preconditions: chartType must be supported and optionJson must be valid ECharts JSON. If the chart dimension is unclear, askUserQuestion first. If multiple chart shapes are plausible, pick the one that best answers the question instead of rendering several.",
            "After Success: the chart is the final answer for this turn — stop. Do not chain another renderChart call, and do not produce explanatory text after it. Any narrative belongs in the description parameter passed to renderChart.",
            "After Failure: fix chartType, optionJson, or the missing chart dimension and retry. Do not invent chart conclusions without a rendered chart.",
            "Do Not Use When: query results are not ready, the user only asked for raw tabular output, or a chart has already been rendered in this turn.",
            "Relation: usually after executeSelectSql. Call activateSkill('chart') before first use in the session."
    })
    @DisallowInPlanMode(ToolNameEnum.RENDER_CHART)
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional explanation for users: chart meaning, key insight(s), and reading guide", required = false)
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
