package edu.zsc.ai.common.constant;

public final class MemoryRecallConstant {

    public static final String FILTER_RECALL_MODE = "recallMode";
    public static final String FILTER_INTENT = "intent";
    public static final String FILTER_SCOPE = "scope";
    public static final String FILTER_MEMORY_TYPE = "memoryType";
    public static final String FILTER_SUB_TYPE = "subType";

    public static final String RESULT_SUMMARY = "summary";
    public static final String RESULT_APPLIED_FILTERS = "appliedFilters";
    public static final String RESULT_ITEMS = "items";

    public static final String ITEM_MEMORY_ID = "memoryId";
    public static final String ITEM_SCOPE = "scope";
    public static final String ITEM_WORKSPACE_LEVEL = "workspaceLevel";
    public static final String ITEM_WORKSPACE_CONTEXT_KEY = "workspaceContextKey";
    public static final String ITEM_MEMORY_TYPE = "memoryType";
    public static final String ITEM_SUB_TYPE = "subType";
    public static final String ITEM_TITLE = "title";
    public static final String ITEM_CONTENT = "content";
    public static final String ITEM_REASON = "reason";
    public static final String ITEM_REVIEW_STATE = "reviewState";
    public static final String ITEM_SOURCE_TYPE = "sourceType";
    public static final String ITEM_SCORE = "score";
    public static final String ITEM_ACTION = "action";

    public static final String NO_MATCH_SUMMARY = "No durable memory matched the current recall filters.";
    public static final String RECALL_SUMMARY_PREFIX = "Recalled ";
    public static final String RECALL_SUMMARY_SUFFIX = " durable memory item(s) across ";
    public static final String RECALL_SUMMARY_END = ".";

    private MemoryRecallConstant() {
    }
}
