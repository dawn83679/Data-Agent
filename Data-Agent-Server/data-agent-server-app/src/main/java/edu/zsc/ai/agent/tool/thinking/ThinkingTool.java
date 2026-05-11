package edu.zsc.ai.agent.tool.thinking;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ThinkingStageEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AgentTool
@Slf4j
public class ThinkingTool {

    @Tool({
            "价值：记录一条用户可见、可审计的数据任务判断。",
            "使用时机：需要说明数据范围、对象选择、SQL 依据、澄清点、写入风险、空结果恢复或结论前提。",
            "前置条件：summary 和 nextAction 必须是面向当前数据任务的具体判断；证据不足时明确写 openQuestions 或 risks。",
            "结果：返回结构化数据决策记录，供用户查看并供后续步骤参考。",
            "边界：不要写隐藏推理链、情绪化草稿、无证据猜测或不影响数据任务的内部想法。"
    })
    public AgentToolResult thinking(
            @P("决策阶段：SCOPE、OBJECT、SQL、CLARIFICATION、WRITE_RISK、RESULT、RECOVERY 或 OTHER。")
            ThinkingStageEnum stage,
            @P("当前数据任务判断结论。必须简短、具体、用户可读。")
            String summary,
            @P(value = "已确认事实，例如连接、database/catalog、schema、对象、字段、业务口径。", required = false)
            List<String> confirmedFacts,
            @P(value = "支撑判断的证据，例如用户确认、工具结果摘要、已验证 SQL 结果。", required = false)
            List<String> evidence,
            @P(value = "仍不确定且会影响下一步的问题。没有则传空列表或省略。", required = false)
            List<String> openQuestions,
            @P(value = "误查、误改、SQL 范围、业务口径或结果解释风险。没有则传空列表或省略。", required = false)
            List<String> risks,
            @P("下一步动作，例如继续查表结构、询问用户、生成 SQL、执行只读查询或等待写入确认。")
            String nextAction,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION)
            String description,
            InvocationParameters parameters) {

        ThinkingStageEnum normalizedStage = stage == null ? ThinkingStageEnum.OTHER : stage;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stage", normalizedStage.name());
        result.put("summary", safeString(summary));
        result.put("confirmedFacts", safeList(confirmedFacts));
        result.put("evidence", safeList(evidence));
        result.put("openQuestions", safeList(openQuestions));
        result.put("risks", safeList(risks));
        result.put("nextAction", safeString(nextAction));
        result.put("description", safeString(description));

        log.info("[Tool done] thinking, stage={}, summary={}, nextAction={}",
                normalizedStage, summary, nextAction);
        return AgentToolResult.success(result, ToolMessageSupport.sentence(
                "数据决策记录已生成。",
                "继续执行 nextAction；不要把这条记录本身当成最终答复。"
        ));
    }

    private static List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
