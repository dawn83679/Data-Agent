package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.Set;

/**
 * Canonical tool names — the method names that LangChain4j exposes to the model.
 * Use these constants wherever tool names are referenced to avoid string literals.
 */
@Getter
@RequiredArgsConstructor
public enum ToolNameEnum {

    // ── Orchestration (SubAgent) ──
    CALLING_EXPLORER_SUB_AGENT("callingExplorerSubAgent"),
    CALLING_PLANNER_SUB_AGENT("callingPlannerSubAgent"),

    // ── Plan mode ──
    ENTER_PLAN_MODE("enterPlanMode"),
    EXIT_PLAN_MODE("exitPlanMode"),

    // ── SQL execution ──
    EXECUTE_SELECT_SQL("executeSelectSql"),
    EXECUTE_NON_SELECT_SQL("executeNonSelectSql"),

    // ── User interaction ──
    ASK_USER_QUESTION("askUserQuestion"),

    // ── Discovery (database objects) ──
    GET_DATABASES("getDatabases"),
    GET_SCHEMAS("getSchemas"),
    SEARCH_OBJECTS("searchObjects"),
    GET_OBJECT_DETAIL("getObjectDetail"),

    // ── Visualization ──
    RENDER_CHART("renderChart"),

    // ── File export ──
    EXPORT_FILE("exportFile"),

    // ── Task management ──
    TODO_WRITE("todoWrite"),

    // ── Skill ──
    ACTIVATE_SKILL("activateSkill"),

    // ── Memory ──
    READ_MEMORY("readMemory"),
    UPDATE_MEMORY("updateMemory"),
    ;

    private final String toolName;

    private static final Set<String> SUB_AGENT_TOOLS = Set.of(
            CALLING_EXPLORER_SUB_AGENT.toolName,
            CALLING_PLANNER_SUB_AGENT.toolName
    );

    public static Optional<ToolNameEnum> fromToolName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        for (ToolNameEnum tool : values()) {
            if (tool.toolName.equals(name)) {
                return Optional.of(tool);
            }
        }
        return Optional.empty();
    }

    public static boolean isSubAgentTool(String name) {
        return name != null && SUB_AGENT_TOOLS.contains(name);
    }
}
