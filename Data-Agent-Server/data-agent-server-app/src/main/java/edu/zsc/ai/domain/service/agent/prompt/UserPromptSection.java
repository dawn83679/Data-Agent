package edu.zsc.ai.domain.service.agent.prompt;

import java.util.List;

public enum UserPromptSection {

    SYSTEM_CONTEXT("{{SYSTEM_CONTEXT}}"),
    TASK("{{TASK}}"),
    SCOPE_HINTS("{{SCOPE_HINTS}}"),
    RESPONSE_PREFERENCES("{{RESPONSE_PREFERENCES}}"),
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
        return List.of(SYSTEM_CONTEXT, TASK, SCOPE_HINTS, RESPONSE_PREFERENCES, DURABLE_FACTS, EXPLICIT_REFERENCES);
    }
}
