package edu.zsc.ai.domain.service.agent.prompt;

import java.util.List;

public enum UserPromptSection {

    SYSTEM_CONTEXT("{{SYSTEM_CONTEXT}}"),
    USER_MEMORY("{{USER_MEMORY}}"),
    USER_PREFERENCES("{{USER_PREFERENCES}}"),
    USER_MENTION("{{USER_MENTION}}"),
    USER_QUESTION("{{USER_QUESTION}}");

    private final String placeholder;

    UserPromptSection(String placeholder) {
        this.placeholder = placeholder;
    }

    public String placeholder() {
        return placeholder;
    }

    public static List<UserPromptSection> renderOrder() {
        return List.of(SYSTEM_CONTEXT, USER_MEMORY, USER_PREFERENCES, USER_MENTION, USER_QUESTION);
    }
}
