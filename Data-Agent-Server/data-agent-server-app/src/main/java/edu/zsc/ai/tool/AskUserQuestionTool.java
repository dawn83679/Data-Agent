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
            value = "Ask the user one or multiple questions when you need to clarify ambiguous requests or obtain missing information. "
                    + "Each question should have 2-3 options (maximum 3). Users can select options and/or provide custom input. "
                    + "Use allowMultiSelect=true for multi-select (checkboxes), false for single-select (radio buttons, default). "
                    + "WHEN TO USE: "
                    + "1. User mentions 'the connection' but there are multiple connections available - ask which one they mean. "
                    + "2. User asks to 'query the database' without specifying which database - list available databases. "
                    + "3. User requests an action but critical information (table name, connection ID, database name) is missing - prompt for specifics. "
                    + "4. You need user confirmation before executing destructive operations (DROP, DELETE, TRUNCATE). "
                    + "EXAMPLES: "
                    + "'Which connection do you want to use?' → options: [list of available connections from getMyConnections] | "
                    + "'Which table should I query?' → options: [list of tables in current schema from getTableNames] | "
                    + "'Confirm deletion of table users?' → options: ['Yes, delete it', 'No, cancel']. "
                    + "After receiving user's response, interpret their answers and continue with the requested operation.",
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public List<UserQuestion> askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        log.info("[Tool] askUserQuestion, {} question(s)", questions == null ? 0 : questions.size());

        return questions;
    }
}
