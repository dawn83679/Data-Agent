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
                    "Asks the user one or more questions with 2-3 options each. ",
                    "Use at decision points: which table/database, time range, filters, ",
                    "visualization dimension, write scope.",
                    "",
                    "Use when: ambiguous target, multiple candidates, missing constraints.",
                    "Skip when: instructions are clear and complete."
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
