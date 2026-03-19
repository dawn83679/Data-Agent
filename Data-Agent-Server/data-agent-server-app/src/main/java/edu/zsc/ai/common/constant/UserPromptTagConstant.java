package edu.zsc.ai.common.constant;

public final class UserPromptTagConstant {

    public static final String SYSTEM_CONTEXT = "system_context";
    public static final String SYSTEM_REMIDER = "system_remider";
    public static final String USER_MEMORY = "user_memory";
    public static final String USER_MENTION = "user_mention";
    public static final String USER_QUESTION = "user_question";

    public static final String SYSTEM_CONTEXT_OPEN = open(SYSTEM_CONTEXT);
    public static final String SYSTEM_CONTEXT_CLOSE = close(SYSTEM_CONTEXT);
    public static final String SYSTEM_REMIDER_OPEN = open(SYSTEM_REMIDER);
    public static final String SYSTEM_REMIDER_CLOSE = close(SYSTEM_REMIDER);
    public static final String USER_MEMORY_OPEN = open(USER_MEMORY);
    public static final String USER_MEMORY_CLOSE = close(USER_MEMORY);
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
