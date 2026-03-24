package edu.zsc.ai.domain.service.agent.prompt;

import java.util.List;

public enum UserPromptSection {

    SYSTEM_CONTEXT("{{SYSTEM_CONTEXT}}"),
    TASK("{{TASK}}"),
    RESPONSE_PREFERENCES("{{RESPONSE_PREFERENCES}}"),
    SCOPE_HINTS("{{SCOPE_HINTS}}"),
    DURABLE_FACTS("{{DURABLE_FACTS}}"),
    EXPLICIT_REFERENCES("{{EXPLICIT_REFERENCES}}");

    private final String placeholder;

    UserPromptSection(String placeholder) {
        this.placeholder = placeholder;
    }

    public String placeholder() {
        return placeholder;
    }

    public static List<UserPromptSection> renderOrder() {
        return List.of(SYSTEM_CONTEXT, TASK, RESPONSE_PREFERENCES, SCOPE_HINTS, DURABLE_FACTS, EXPLICIT_REFERENCES);
    }
}
