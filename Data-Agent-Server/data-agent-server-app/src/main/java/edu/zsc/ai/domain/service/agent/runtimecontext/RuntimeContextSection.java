package edu.zsc.ai.domain.service.agent.runtimecontext;

import java.util.List;

public enum RuntimeContextSection {

    SYSTEM_CONTEXT("{{SYSTEM_CONTEXT}}"),
<<<<<<< HEAD
=======
    AVAILABLE_CONNECTIONS("{{AVAILABLE_CONNECTIONS}}"),
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
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
<<<<<<< HEAD
        return List.of(SYSTEM_CONTEXT, SCOPE_HINTS, RESPONSE_PREFERENCES, DURABLE_FACTS, EXPLICIT_REFERENCES);
=======
        return List.of(SYSTEM_CONTEXT, AVAILABLE_CONNECTIONS, SCOPE_HINTS, RESPONSE_PREFERENCES, DURABLE_FACTS, EXPLICIT_REFERENCES);
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    }
}
