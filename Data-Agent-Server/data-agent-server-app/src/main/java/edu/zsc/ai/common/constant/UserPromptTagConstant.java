package edu.zsc.ai.common.constant;

public final class UserPromptTagConstant {

    public static final String SYSTEM_CONTEXT = "system_context";
    public static final String TASK = "task";
    public static final String RESPONSE_PREFERENCES = "response_preferences";
    public static final String SCOPE_HINTS = "scope_hints";
    public static final String DURABLE_FACTS = "durable_facts";
    public static final String EXPLICIT_REFERENCES = "explicit_references";

    // Legacy wrappers kept for backward compatibility in stored messages and strip logic.
    public static final String USER_MEMORY = "user_memory";
    public static final String USER_PREFERENCES = "user_preferences";
    public static final String USER_MEMORY_CONTEXT = "user_memory_context";
    public static final String USER_MENTION = "user_mention";
    public static final String USER_QUESTION = "user_question";

    public static final String SYSTEM_CONTEXT_OPEN = open(SYSTEM_CONTEXT);
    public static final String SYSTEM_CONTEXT_CLOSE = close(SYSTEM_CONTEXT);
    public static final String TASK_OPEN = open(TASK);
    public static final String TASK_CLOSE = close(TASK);
    public static final String RESPONSE_PREFERENCES_OPEN = open(RESPONSE_PREFERENCES);
    public static final String RESPONSE_PREFERENCES_CLOSE = close(RESPONSE_PREFERENCES);
    public static final String SCOPE_HINTS_OPEN = open(SCOPE_HINTS);
    public static final String SCOPE_HINTS_CLOSE = close(SCOPE_HINTS);
    public static final String DURABLE_FACTS_OPEN = open(DURABLE_FACTS);
    public static final String DURABLE_FACTS_CLOSE = close(DURABLE_FACTS);
    public static final String EXPLICIT_REFERENCES_OPEN = open(EXPLICIT_REFERENCES);
    public static final String EXPLICIT_REFERENCES_CLOSE = close(EXPLICIT_REFERENCES);

    public static final String USER_MEMORY_OPEN = open(USER_MEMORY);
    public static final String USER_MEMORY_CLOSE = close(USER_MEMORY);
    public static final String USER_PREFERENCES_OPEN = open(USER_PREFERENCES);
    public static final String USER_PREFERENCES_CLOSE = close(USER_PREFERENCES);
    public static final String USER_MEMORY_CONTEXT_OPEN = open(USER_MEMORY_CONTEXT);
    public static final String USER_MEMORY_CONTEXT_CLOSE = close(USER_MEMORY_CONTEXT);
    public static final String USER_MENTION_OPEN = open(USER_MENTION);
    public static final String USER_MENTION_CLOSE = close(USER_MENTION);
    public static final String USER_QUESTION_OPEN = open(USER_QUESTION);
    public static final String USER_QUESTION_CLOSE = close(USER_QUESTION);

    private UserPromptTagConstant() {
    }

    public static String open(String tagName) {
        return "<" + tagName + ">";
    }

    public static String close(String tagName) {
        return "</" + tagName + ">";
    }
}
