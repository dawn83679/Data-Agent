package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool that lets the model ask the user a question with optional choices and/or free-text input.
 * Marked IMMEDIATE so the stream ends after this tool runs; the frontend shows the question
 * and submits the user's answer as a new message to continue the conversation.
 */
@Component
@Slf4j
public class AskUserQuestionTool {

    private static final int MAX_OPTIONS = 3;

    @Tool(
            value = "Ask the user a question when you need their input to proceed. "
                    + "Provide a clear question and optionally up to 3 choices and/or a hint for free-text answer. "
                    + "Use this when you need confirmation, preference, or any user decision before continuing.",
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String askUserQuestion(
            @P("The question to show to the user. Be clear and concise.") String question,
            @P(value = "Optional list of up to 3 choices the user can pick from. Omit or pass empty if only free text is needed.", required = false)
            List<String> options,
            @P(value = "Optional hint or placeholder for a free-text input (e.g. 'Enter your name'). Omit if only choices are used.", required = false)
            String freeTextHint) {
        log.info("[Tool] askUserQuestion, question={}, optionsSize={}", question, options != null ? options.size() : 0);

        List<String> opts = options != null && !options.isEmpty()
                ? options.stream().limit(MAX_OPTIONS).toList()
                : List.of();

        Map<String, Object> out = new HashMap<>();
        out.put("question", question != null ? question : "");
        out.put("options", opts);
        out.put("freeTextHint", freeTextHint != null ? freeTextHint : "");

        return JsonUtil.object2json(out);
    }
}
