package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.model.UserQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

@AgentTool
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = {
                    "价值：让用户在具体选项中选择，解决歧义，避免模型猜测。",
                    "使用时机：一个简短澄清问题可以推进任务、缩小范围或在有意义的备选项中做选择。",
                    "前置条件：每个问题建议包含 2 到 3 个选项。",
                    "结果：暂停本轮，等待用户回复。",
                    "边界：只问高价值问题；不要用它逃避可执行的验证。"
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("要询问用户的问题；每个问题建议包含 2 到 3 个选项。")
            List<UserQuestion> questions,
            InvocationParameters parameters) {

        int count = CollectionUtils.size(questions);
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        return "已向用户发送 " + count
                + " 个问题。等待用户回复后，再做依赖这些答案的决定；不要在答案缺失时替用户做需要确认的选择。";
    }
}
