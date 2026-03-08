package edu.zsc.ai.common.enums.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical tool names — the method names that LangChain4j exposes to the model.
 * Use these constants wherever tool names are referenced to avoid string literals.
 */
@Getter
@RequiredArgsConstructor
public enum ToolNameEnum {

    // ── Plan mode ──
    ENTER_PLAN_MODE("enterPlanMode"),
    EXIT_PLAN_MODE("exitPlanMode"),

    // ── SQL execution ──
    EXECUTE_SELECT_SQL("executeSelectSql"),
    EXECUTE_NON_SELECT_SQL("executeNonSelectSql"),

    // ── User interaction ──
    ASK_USER_QUESTION("askUserQuestion"),
    ASK_USER_CONFIRM("askUserConfirm"),

    // ── Database objects ──
    GET_CONNECTIONS("getConnections"),
    GET_CATALOG_NAMES("getCatalogNames"),
    GET_OBJECT_NAMES("getObjectNames"),
    COUNT_OBJECT_ROWS("countObjectRows"),
    GET_OBJECT_DDL("getObjectDdl"),
    GET_INDEXES("getIndexes"),

    // ── Thinking ──
    THINKING("thinking"),

    // ── Visualization ──
    RENDER_CHART("renderChart"),

    // ── Memory ──
    SEARCH_MEMORIES("searchMemories"),
    LIST_CANDIDATE_MEMORIES("listCandidateMemories"),
    CREATE_CANDIDATE_MEMORY("createCandidateMemory"),
    DELETE_CANDIDATE_MEMORY("deleteCandidateMemory"),

    // ── Task management ──
    TODO_WRITE("todoWrite"),
    ;

    private final String toolName;
}
