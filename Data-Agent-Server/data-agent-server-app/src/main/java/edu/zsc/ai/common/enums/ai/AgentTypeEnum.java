package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Agent types in the multi-agent architecture.
 * <ul>
 *   <li>MAIN — the orchestrator agent that talks to the user, executes SQL, and delegates to sub-agents</li>
 *   <li>EXPLORER — schema discovery sub-agent (TodoTool, SearchObjectsTool, GetObjectDetailTool)</li>
 *   <li>PLANNER — SQL plan generation sub-agent (TodoTool, ActivateSkillTool, GetObjectDetailTool)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AgentTypeEnum {

    MAIN("main"),
    EXPLORER("explorer"),
    PLANNER("planner");

    private final String code;
}
