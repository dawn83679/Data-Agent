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
                    "Use When: useful when a short clarification can unlock progress, reduce the search space, or let the user choose among meaningful alternatives.",
                    "After Success: the user's reply becomes new scope or preference information for the next step.",
                    "After Failure: if you still cannot phrase a helpful question, you may gather more context first and try again later.",
                    "Wait For User: this tool creates an explicit pause point for decisions that depend on the user's choice.",
                    "Relation: often helpful after getEnvironmentOverview, searchObjects, or other discovery steps. Maximum 3 options per question."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
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
