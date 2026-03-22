package edu.zsc.ai.common.constant;

public final class MemoryLogConstant {

    public static final String LOGGER_NAME = "MemoryService";

    public static final String EVENT_MEMORY_SEARCH = "memory_search";
    public static final String EVENT_MEMORY_MANUAL_CREATE = "memory_manual_create";
    public static final String EVENT_MEMORY_MANUAL_UPDATE = "memory_manual_update";
    public static final String EVENT_MEMORY_AGENT_WRITE = "memory_agent_write";
    public static final String EVENT_MEMORY_DISABLE = "memory_disable";
    public static final String EVENT_MEMORY_ENABLE = "memory_enable";
    public static final String EVENT_MEMORY_DELETE = "memory_delete";
    public static final String EVENT_MEMORY_MAINTENANCE_INSPECT = "memory_maintenance_inspect";
    public static final String EVENT_MEMORY_MAINTENANCE_RUN = "memory_maintenance_run";
    public static final String EVENT_MEMORY_ACCESS_RECORDED = "memory_access_recorded";
    public static final String EVENT_MEMORY_EMBEDDING_REBUILD_FAILED = "memory_embedding_rebuild_failed";
    public static final String EVENT_MEMORY_EMBEDDING_REMOVE_FAILED = "memory_embedding_remove_failed";

    public static final String FIELD_MEMORY_ID = "memoryId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_CONVERSATION_ID = "conversationId";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_MEMORY_TYPE = "memoryType";
    public static final String FIELD_SUB_TYPE = "subType";
    public static final String FIELD_SOURCE_TYPE = "sourceType";
    public static final String FIELD_ENABLE = "enable";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_QUERY_TEXT_PRESENT = "queryTextPresent";
    public static final String FIELD_LIMIT = "limit";
    public static final String FIELD_MIN_SCORE = "minScore";
    public static final String FIELD_RESULT_COUNT = "resultCount";
    public static final String FIELD_MEMORY_IDS = "memoryIds";
    public static final String FIELD_PROCESSED_COUNT = "processedCount";
    public static final String FIELD_PROCESSED_DISABLED_COUNT = "processedDisabledCount";
    public static final String FIELD_ENABLED_MEMORY_COUNT = "enabledMemoryCount";
    public static final String FIELD_DISABLED_MEMORY_COUNT = "disabledMemoryCount";
    public static final String FIELD_DUPLICATE_ENABLED_MEMORY_COUNT = "duplicateEnabledMemoryCount";

    private MemoryLogConstant() {
    }
}
