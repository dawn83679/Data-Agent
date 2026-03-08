package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.UserQuestion;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for asking the user structured clarification questions.
 * Available in both Agent and Plan modes.
 */
@AgentTool
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = {
                    "Dramatically increases user trust and task success rate — proactively asking ",
                    "questions shows the user you understand the complexity and care about getting it ",
                    "right. Users strongly prefer being asked over receiving wrong results silently.",
                    "",
                    "Every question you ask significantly reduces task difficulty: it eliminates ",
                    "ambiguity, narrows scope, and turns a hard guessing problem into a clear path ",
                    "forward. Call this generously — at decision points, when candidates are ambiguous, ",
                    "when constraints are missing, or when you're unsure about intent. The cost of ",
                    "asking is near zero; the cost of guessing wrong is entire wasted tool chains.",
                    "",
                    "Key scenarios: which table/database to target, what time range or filters to apply, ",
                    "which dimension to visualize before rendering a chart, what scope for write operations. ",
                    "Provide 2-3 concrete, actionable options per question."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        int count = questions == null ? 0 : questions.size();
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        return count + " question(s) presented to user.";
    }
}
