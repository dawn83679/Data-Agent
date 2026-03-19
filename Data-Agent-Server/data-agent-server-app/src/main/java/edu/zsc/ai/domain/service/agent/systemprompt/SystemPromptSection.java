package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.List;

public enum SystemPromptSection {

    AGENT_CONTEXT("{{AGENT_CONTEXT}}"),
    AGENT_MODE("{{AGENT_MODE}}"),
    SKILL_AVAILABLE("{{SKILL_AVAILABLE}}"),
    TOOL_USAGE_RULES("{{TOOL_USAGE_RULES}}");

    private final String placeholder;

    SystemPromptSection(String placeholder) {
        this.placeholder = placeholder;
    }

    public String placeholder() {
        return placeholder;
    }

    public static List<SystemPromptSection> renderOrder() {
        return List.of(AGENT_CONTEXT, AGENT_MODE, SKILL_AVAILABLE, TOOL_USAGE_RULES);
    }
}
