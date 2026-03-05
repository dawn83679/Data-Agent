package edu.zsc.ai.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
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
            "[WHAT] Create a chart payload for frontend ECharts rendering.",
            "[HOW] Pass chartType + optionJson (+ optional description). optionJson must be a valid JSON object string.",
            "[DESCRIPTION] description should briefly explain chart meaning, key insight(s), and how to read it.",
            "[WHEN] Use after query results are ready and a visual chart is needed."
    })
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional explanation for users: chart meaning, key insight(s), and reading guide", required = false)
            String description,
            InvocationParameters parameters) {
        log.info("[Tool] renderChart, chartType={}", chartType);
        try {
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
            return AgentToolResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error("[Tool error] renderChart, chartType={}", chartType, e);
            return AgentToolResult.fail(e);
        }
    }
}
