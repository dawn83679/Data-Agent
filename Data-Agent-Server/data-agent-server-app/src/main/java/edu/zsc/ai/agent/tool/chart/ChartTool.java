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
            "[GOAL] Produce frontend-renderable chart payload from query results.",
            "[WHEN] Use after executeSelectSql result is ready and user explicitly requests visualization or data clearly benefits from it.",
            "[WHEN_NOT] Do not use when user has not requested visualization and data is simple. Do not call before query data is available. DISABLED in Plan mode — no query data available to visualize.",
            "[INPUT] chartType + optionJson (valid ECharts JSON) + optional description explaining chart meaning and key insights."
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
