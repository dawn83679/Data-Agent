package edu.zsc.ai.tool.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing a single user question with options and optional free-text input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestion {

    /**
     * The question to ask the user (e.g., "Which database do you want to connect to?").
     * Should be clear and specific to help users understand what information you need.
     */
    @Description("The question to ask the user. Should be clear and specific to help users understand what information you need.")
    private String question;

    /**
     * List of options for user to choose from (minimum 3 options required).
     * Example: ["Database A", "Database B", "Database C"]
     * Provide concrete options based on available data (connections, databases, tables, etc.).
     */
    @Description("List of options for user to choose from (minimum 3 options required). Provide concrete options based on available data.")
    private List<String> options;

    /**
     * Optional hint for free-text input field (e.g., "Enter custom database name").
     * If null, no hint is shown. Use this to guide users when they provide custom input.
     */
    @Description("Optional hint for free-text input field. If provided, this text will be shown as placeholder in the custom input field to guide users.")
    private String freeTextHint;
}
