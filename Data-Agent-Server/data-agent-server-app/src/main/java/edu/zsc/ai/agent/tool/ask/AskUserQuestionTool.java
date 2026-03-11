package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.model.UserQuestion;
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
                    "Calling this tool greatly improves task success when 2+ candidates exist — letting the user ",
                    "choose eliminates wrong-target errors and builds trust. ",
                    "Asks structured questions with 2-3 options to eliminate ambiguity and get the right target.",
                    "",
                    "When to Use: when searchObjects or getEnvironmentOverview yields multiple candidates; when scope or intent is unclear; before renderChart to choose dimension.",
                    "When NOT to Use: when there is only one candidate or the user has already specified the target clearly.",
                    "Relation: call after discovery when 2+ candidates; provide concrete options (e.g. table/database choice, time range, chart dimension). Maximum 3 options per question."
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
