package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

import java.util.Locale;

/**
 * Agent execution mode.
 */
@Getter
public enum AgentModeEnum {

    AGENT("agent"),
    PLAN("plan"),
    MULTI_AGENT("multi-agent");

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
        if (MULTI_AGENT.code.equals(normalized)) {
            return MULTI_AGENT;
        }
        if (PLAN.code.equals(normalized)) {
            return PLAN;
        }
        return AGENT;
    }
}
