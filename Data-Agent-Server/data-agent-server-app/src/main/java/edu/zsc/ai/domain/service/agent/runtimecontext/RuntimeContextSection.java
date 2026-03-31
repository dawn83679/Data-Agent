package edu.zsc.ai.domain.service.agent.runtimecontext;

import java.util.List;

public enum RuntimeContextSection {

    SYSTEM_CONTEXT("{{SYSTEM_CONTEXT}}"),
    AVAILABLE_CONNECTIONS("{{AVAILABLE_CONNECTIONS}}"),
    SCOPE_HINTS("{{SCOPE_HINTS}}"),
    RESPONSE_PREFERENCES("{{RESPONSE_PREFERENCES}}"),
    DURABLE_FACTS("{{DURABLE_FACTS}}"),
    EXPLICIT_REFERENCES("{{EXPLICIT_REFERENCES}}");

    private final String placeholder;

    RuntimeContextSection(String placeholder) {
        this.placeholder = placeholder;
    }

    public String placeholder() {
        return placeholder;
    }

    public static List<RuntimeContextSection> renderOrder() {
        return List.of(SYSTEM_CONTEXT, AVAILABLE_CONNECTIONS, SCOPE_HINTS, RESPONSE_PREFERENCES, DURABLE_FACTS, EXPLICIT_REFERENCES);
    }
}
