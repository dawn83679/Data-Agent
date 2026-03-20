package edu.zsc.ai.common.constant;

public final class MemoryRecallPlanningConstant {

    public static final String QUERY_NAME_PREFIX = "memory-recall";
    public static final String LEGACY_QUERY_NAME = "legacy";
    public static final String LEGACY_PLANNING_REASON = "legacy_overload";

    public static final String REASON_EXPLICIT_SCOPE = "explicit_scope";
    public static final String REASON_FALLBACK_DEFAULT_SCOPE_PLAN = "fallback_default_scope_plan";
    public static final String REASON_MEMORY_TYPE_PREFERENCE_DEFAULT = "memory_type_preference_default";
    public static final String REASON_MEMORY_TYPE_BUSINESS_RULE_DEFAULT = "memory_type_business_rule_default";
    public static final String REASON_MEMORY_TYPE_KNOWLEDGE_POINT_DEFAULT = "memory_type_knowledge_point_default";
    public static final String REASON_MEMORY_TYPE_GOLDEN_SQL_CASE_DEFAULT = "memory_type_golden_sql_case_default";
    public static final String REASON_MEMORY_TYPE_WORKFLOW_CONSTRAINT_DEFAULT = "memory_type_workflow_constraint_default";
    public static final String REASON_SUBTYPE_REORDERED_TO_USER = "subtype_reordered_to_user";
    public static final String REASON_SUBTYPE_REORDERED_TO_WORKSPACE = "subtype_reordered_to_workspace";
    public static final String REASON_SUBTYPE_REORDERED_TO_CONVERSATION = "subtype_reordered_to_conversation";

    private MemoryRecallPlanningConstant() {
    }
}
