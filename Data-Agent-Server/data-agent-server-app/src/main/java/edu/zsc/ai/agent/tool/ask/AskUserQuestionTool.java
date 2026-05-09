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

/**
 * Tool for asking the user structured clarification questions.
 * Available in both Agent and Plan modes.
 */
@AgentTool
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = {
                    "Value: resolves ambiguity by letting the user choose among concrete options instead of letting the model guess.",
                    "Use When: one short clarification can unlock progress, reduce scope, or choose among meaningful alternatives.",
                    "Preconditions: each question should have 2-3 options.",
                    "Result: pauses the turn until the user replies.",
                    "Boundary: ask high-value questions only; do not use this to avoid available verification."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("Questions to ask; each should have 2-3 options.")
            List<UserQuestion> questions,
            InvocationParameters parameters) {

        int count = CollectionUtils.size(questions);
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        if (isChinese(parameters)) {
            return "已向用户发送 " + count
                    + " 个问题。等待用户回复后，再做依赖这些答案的决定；不要在答案缺失时替用户做需要确认的选择。";
        }
        return count + " question(s) were sent to the user. Wait for the user's reply before making any decision that depends on these answers. Do not make user-dependent choices on the user's behalf while the answers are still pending.";
    }

    private boolean isChinese(InvocationParameters parameters) {
        if (parameters == null) {
            return false;
        }
        String language = parameters.getOrDefault(InvocationContextConstant.LANGUAGE, "en");
        return StringUtils.startsWithIgnoreCase(language, "zh");
    }
}
