package edu.zsc.ai.common.constant;

public final class MemoryRecallLogConstant {

    public static final String LOGGER_NAME = "MemoryRecall";

    public static final String EVENT_RECALL_START = "memory_recall_start";
    public static final String EVENT_RECALL_PLANNED = "memory_recall_planned";
    public static final String EVENT_RECALL_QUERY_DISPATCH = "memory_recall_query_dispatch";
    public static final String EVENT_RECALL_QUERY_HANDLER_MATCH = "memory_recall_query_handler_match";
    public static final String EVENT_RECALL_QUERY_RESULT = "memory_recall_query_result";
    public static final String EVENT_RECALL_POST_PROCESS = "memory_recall_post_process";
    public static final String EVENT_RECALL_COMPLETE = "memory_recall_complete";

    public static final String FIELD_RECALL_MODE = "recallMode";
    public static final String FIELD_QUERY_TEXT = "queryText";
    public static final String FIELD_QUERY_TEXT_PRESENT = "queryTextPresent";
    public static final String FIELD_REQUESTED_SCOPE = "requestedScope";
    public static final String FIELD_REQUESTED_MEMORY_TYPE = "requestedMemoryType";
    public static final String FIELD_REQUESTED_SUB_TYPE = "requestedSubType";
    public static final String FIELD_PLANNED_QUERIES = "plannedQueries";
    public static final String FIELD_QUERY_NAME = "queryName";
    public static final String FIELD_PLANNING_REASON = "planningReason";
    public static final String FIELD_TARGET_SCOPE = "targetScope";
    public static final String FIELD_QUERY_STRATEGY = "queryStrategy";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_MATCHED_HANDLER = "matchedHandler";
    public static final String FIELD_RESULT_COUNT = "resultCount";
    public static final String FIELD_RESULT_MEMORY_IDS = "resultMemoryIds";
    public static final String FIELD_MERGED_COUNT = "mergedCount";
    public static final String FIELD_DEDUPLICATED_COUNT = "deduplicatedCount";
    public static final String FIELD_FINAL_COUNT = "finalCount";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_APPLIED_FILTERS = "appliedFilters";
    public static final String FIELD_USED_FALLBACK = "usedFallback";
    public static final String FIELD_EXECUTION_PATH = "executionPath";

    public static final String EXECUTION_PATH_BROWSE = "browse";
    public static final String EXECUTION_PATH_SEMANTIC = "semantic";
    public static final String EXECUTION_PATH_SEMANTIC_EMPTY_BROWSE_FALLBACK = "semantic_empty_browse_fallback";
    public static final String EXECUTION_PATH_HYBRID_SEMANTIC = "hybrid_semantic";
    public static final String EXECUTION_PATH_HYBRID_BROWSE_FALLBACK = "hybrid_browse_fallback";
    public static final String EXECUTION_PATH_HYBRID_CONVERSATION_BROWSE_FALLBACK = "hybrid_conversation_browse_fallback";
    public static final String EXECUTION_PATH_SEMANTIC_EXCEPTION_FALLBACK = "semantic_exception_fallback";

    private MemoryRecallLogConstant() {
    }
}
