package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.model.UserQuestion;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@AgentTool
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = {
                    "价值：暂停本轮向用户收集会影响下一步的回答，避免模型猜测。",
                    "使用时机：继续执行依赖用户回答，会影响环境、范围、对象、SQL、写操作或最终结论。",
                    "前置条件：每个问题清楚具体；优先给 2 到 3 个选项，无法枚举时提供 freeTextHint。",
                    "结果：立即暂停本轮，等待用户回复后再继续当前任务。",
                    "边界：不要在最终答复中列问题替代本工具；不影响下一步的说明性问题不用问。"
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("要询问用户的问题；每个问题建议包含 2 到 3 个选项。")
            List<UserQuestion> questions,
            InvocationParameters parameters) {

        int count = CollectionUtils.size(questions);
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        if (!isChinese(parameters)) {
            return count + " question(s) were sent to the user. Wait for the user's reply before continuing; "
                    + "do not make decisions that depend on missing answers.";
        }
        return "已向用户发送 " + count
                + " 个问题。等待用户回复后，再做依赖这些答案的决定；不要在答案缺失时替用户做需要确认的选择。";
    }

    private boolean isChinese(InvocationParameters parameters) {
        if (parameters == null) {
            return false;
        }
        Object language = parameters.get(InvocationContextConstant.LANGUAGE);
        return StringUtils.startsWithIgnoreCase(String.valueOf(language), "zh");
    }
}
