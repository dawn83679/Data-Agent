package edu.zsc.ai.common.constant;

public final class MemoryLogConstant {

    public static final String LOGGER_NAME = "MemoryService";

    public static final String EVENT_MEMORY_SEARCH = "memory_search";
    public static final String EVENT_MEMORY_MANUAL_CREATE = "memory_manual_create";
    public static final String EVENT_MEMORY_MANUAL_UPDATE = "memory_manual_update";
    public static final String EVENT_MEMORY_AGENT_WRITE = "memory_agent_write";
    public static final String EVENT_MEMORY_ARCHIVE = "memory_archive";
    public static final String EVENT_MEMORY_RESTORE = "memory_restore";
    public static final String EVENT_MEMORY_DELETE = "memory_delete";
    public static final String EVENT_MEMORY_MAINTENANCE_INSPECT = "memory_maintenance_inspect";
    public static final String EVENT_MEMORY_MAINTENANCE_RUN = "memory_maintenance_run";
    public static final String EVENT_MEMORY_ACCESS_RECORDED = "memory_access_recorded";
    public static final String EVENT_MEMORY_USAGE_RECORDED = "memory_usage_recorded";
    public static final String EVENT_MEMORY_EMBEDDING_REBUILD_FAILED = "memory_embedding_rebuild_failed";
    public static final String EVENT_MEMORY_EMBEDDING_REMOVE_FAILED = "memory_embedding_remove_failed";

    public static final String FIELD_MEMORY_ID = "memoryId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_CONVERSATION_ID = "conversationId";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_WORKSPACE_LEVEL = "workspaceLevel";
    public static final String FIELD_WORKSPACE_CONTEXT_KEY = "workspaceContextKey";
    public static final String FIELD_MEMORY_TYPE = "memoryType";
    public static final String FIELD_SUB_TYPE = "subType";
    public static final String FIELD_SOURCE_TYPE = "sourceType";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_QUERY_TEXT_PRESENT = "queryTextPresent";
    public static final String FIELD_LIMIT = "limit";
    public static final String FIELD_MIN_SCORE = "minScore";
    public static final String FIELD_RESULT_COUNT = "resultCount";
    public static final String FIELD_MEMORY_IDS = "memoryIds";
    public static final String FIELD_PROCESSED_COUNT = "processedCount";
    public static final String FIELD_PROCESSED_ARCHIVED_COUNT = "processedArchivedCount";
    public static final String FIELD_PROCESSED_HIDDEN_COUNT = "processedHiddenCount";
    public static final String FIELD_ACTIVE_MEMORY_COUNT = "activeMemoryCount";
    public static final String FIELD_ARCHIVED_MEMORY_COUNT = "archivedMemoryCount";
    public static final String FIELD_HIDDEN_MEMORY_COUNT = "hiddenMemoryCount";
    public static final String FIELD_EXPIRED_ACTIVE_MEMORY_COUNT = "expiredActiveMemoryCount";
    public static final String FIELD_DUPLICATE_ACTIVE_MEMORY_COUNT = "duplicateActiveMemoryCount";

    private MemoryLogConstant() {
    }
}
