package edu.zsc.ai.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.tool.model.UserQuestion;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for asking the user one or multiple questions with options and/or free-text input.
 * Uses ReturnBehavior.IMMEDIATE to pause the conversation until user responds.
 * Supports both single and multi-question formats.
 */
@Component
@Slf4j
public class AskUserQuestionTool {

    @Tool(
            value = "[WHAT] Ask the user one or more questions with structured choices and optional free-text input. "
                    + "[WHEN] Use when: (1) user intent is ambiguous or critical information is missing; "
                    + "(2) a write operation requires confirmation; "
                    + "(3) a decision must be made before proceeding. "
                    + "IMPORTANT — YOU MUST call this tool before ANY write operation (INSERT, UPDATE, DELETE, DDL). "
                    + "NEVER skip confirmation even if the intent seems obvious. "
                    + "[HOW] Each question should have 2-3 options (maximum 3). "
                    + "Users can select options and/or provide custom input. "
                    + "Use allowMultiSelect=true for multi-select (checkboxes), false for single-select (radio buttons, default). "
                    + "After receiving the response, interpret the answers and continue the operation.",
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public List<UserQuestion> askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        log.info("[Tool] askUserQuestion, {} question(s)", questions == null ? 0 : questions.size());

        return questions;
    }
}
