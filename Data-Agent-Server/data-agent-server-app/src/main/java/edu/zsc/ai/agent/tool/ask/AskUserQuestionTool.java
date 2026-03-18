package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.ask.model.UserQuestion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

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
                    "Use When: call when discovery or planning leaves multiple plausible targets, or when a missing preference materially changes the outcome.",
                    "After Success: wait for the user's reply, use the answer to narrow scope, and then continue with the next tool.",
                    "After Failure: if you cannot frame concrete options yet, do more discovery first. Do not ask vague questions or decide on the user's behalf.",
                    "Wait For User: do not continue discovery, planning, execution, or chart selection until the user answers.",
                    "Relation: often after getEnvironmentOverview or searchObjects, and sometimes before renderChart or callingPlannerSubAgent. Maximum 3 options per question."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        int count = CollectionUtils.size(questions);
        log.info("[Tool] askUserQuestion, {} question(s)", count);
        return ToolMessageSupport.sentence(
                count + " question(s) were sent to the user.",
                ToolMessageSupport.waitForUserReply("making any decision that depends on these answers"),
                "Do not proceed with discovery, planning, or execution on the user's behalf."
        );
    }
}
