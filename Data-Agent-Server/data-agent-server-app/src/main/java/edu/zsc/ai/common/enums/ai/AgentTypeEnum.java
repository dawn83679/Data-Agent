package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

/**
 * Agent types in the multi-agent architecture.
 * <ul>
 *   <li>MAIN — the orchestrator agent that talks to the user, executes SQL, and delegates to sub-agents</li>
 *   <li>EXPLORER — schema discovery sub-agent (TodoTool, SearchObjectsTool, GetObjectDetailTool)</li>
 *   <li>PLANNER — SQL plan generation sub-agent (TodoTool, GetObjectDetailTool, ExecuteSqlTool)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AgentTypeEnum {

    MAIN("main"),
    EXPLORER("explorer"),
    PLANNER("planner");

    private final String code;

    public static AgentTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MAIN;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (AgentTypeEnum value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return MAIN;
    }
}
