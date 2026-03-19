package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

import java.util.Locale;

/**
 * Agent execution mode: AGENT (execute SQL) or PLAN (analyze and plan only).
 */
@Getter
public enum AgentModeEnum {

    AGENT("agent"),
    PLAN("plan");

    private final String code;

    AgentModeEnum(String code) {
        this.code = code;
    }

    /**
     * Resolve request agentType to mode enum.
     * Null, blank, or unknown values default to AGENT.
     */
    public static AgentModeEnum fromRequest(String agentType) {
        if (agentType == null || agentType.isBlank()) {
            return AGENT;
        }
        String normalized = agentType.trim().toLowerCase(Locale.ROOT);
        if (PLAN.code.equals(normalized)) {
            return PLAN;
        }
        return AGENT;
    }

    public String promptMode() {
        return this == PLAN ? "plan" : "normal";
    }
}
