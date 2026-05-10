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
            "价值：把已验证数据渲染成一个 ECharts 图表。",
            "使用时机：数据已经准备好，且用户需要图表、趋势、对比或分布展示。",
            "前置条件：先激活图表技能；chartType 必须受支持；optionJson 必须是合法 ECharts JSON。",
            "结果：图表就是本轮最终交付物。",
            "边界：每轮最多渲染一个图表；工具调用成功后不要再输出助手文本。"
    })
    @DisallowInPlanMode(ToolNameEnum.RENDER_CHART)
    public AgentToolResult renderChart(
            @P("图表类型：LINE、BAR、PIE、SCATTER、AREA") String chartType,
            @P("ECharts option JSON 字符串，必须是合法 JSON 对象。") String optionJson,
            @P("必填。面向用户的图表说明。因为图表工具成功后不应再追加助手文本，所以洞察或阅读指引都写在这里。")
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
                    "renderChart 输入无效：" + e.getMessage()
                            + "。chartType 必须是 LINE、BAR、PIE、SCATTER、AREA 之一，optionJson 必须是合法 ECharts JSON。"
                            + "修正图表输入后重试；没有成功渲染图表前不要编造图表结论。"
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
                "图表渲染 payload 已准备好。",
                "图表就是本轮最终答案。立即结束本轮，不要在工具调用后输出任何描述、总结或评论图表的助手文本。任何洞察或阅读指引都应已经写入 description 参数。"
        ));
    }
}
