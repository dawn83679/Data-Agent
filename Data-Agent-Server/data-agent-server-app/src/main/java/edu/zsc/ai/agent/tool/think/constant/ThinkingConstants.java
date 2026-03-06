package edu.zsc.ai.agent.tool.think.constant;

public final class ThinkingConstants {

    private ThinkingConstants() {
    }

    public static final String PLACEHOLDER_MISSING = "[MISSING]";
    public static final String CONTENT_SELECTED_CANDIDATE_PREFIX = "selectedCandidateId=";

    public static final String ACTION_RESPOND = "respond";
    public static final String ACTION_CONTINUE_THINKING = "continue-thinking";
    public static final String ACTION_ASK_USER_QUESTION = "askUserQuestion";
    public static final String ACTION_ASK_USER_CONFIRM = "askUserConfirm";
    public static final String ACTION_EXECUTE_NON_SELECT_SQL = "executeNonSelectSql";

    public static final String QUESTION_TYPE_CLARIFICATION = "intent_or_scope_clarification";

    public static final String INSTRUCTION_PROCEED_STAGE_FLOW = "proceed_with_stage_flow";
    public static final String INSTRUCTION_SELF_CORRECT_THEN_RETRY = "self_correct_then_retry";

    public static final String MODE_KEEP = "KEEP";

    public static final String MEMORY_TYPE_SCHEMA_DISAMBIGUATION = "schema_disambiguation_preference";
    public static final String MEMORY_TYPE_WORKLOAD_SQL_HINT = "workload_sql_hint";
    public static final String MEMORY_TYPE_QUERY_PREFERENCE = "query_preference_hint";

    public static final String MEMORY_REASON_SELECTED_CANDIDATE = "User selected a candidate.";
    public static final String MEMORY_REASON_CORRECTED_SQL = "User corrected SQL.";
    public static final String MEMORY_REASON_PREFERENCE_HINT = "User preference hint.";

    public static final String REASON_LOOP_TERMINATED = "Reasoning loop terminated";
    public static final String REASON_NEED_USER_CLARIFICATION = "Need user clarification";
    public static final String REASON_WRITE_CONFIRMATION_REQUIRED = "Write confirmation required";
    public static final String REASON_WRITE_CONFIRMED = "Write confirmed";
    public static final String REASON_DEFAULT_STAGE = "Default stage transition";

    public static final String TRACE_NEXT_THOUGHT_STOP = "nextThoughtNeeded=false -> respond";
    public static final String TRACE_NEED_USER_QUESTION = "needUserQuestion=true -> askUserQuestion";
    public static final String TRACE_WRITE_SAFETY_CONFIRM = "write safety gate -> askUserConfirm";
    public static final String TRACE_EXECUTION_ERROR = "execution error -> verify/correct";
    public static final String TRACE_WRITE_CONFIRMED = "write confirmed -> executeNonSelectSql";
    public static final String TRACE_DEFAULT_TRANSITION = "default stage transition";

    public static final String DIMENSION_PROBLEM = "problem";
    public static final String DIMENSION_TRIGGER = "trigger";
    public static final String DIMENSION_STATE = "state";
    public static final String DIMENSION_DECOMPOSE = "decompose";
    public static final String DIMENSION_GENERATE = "generate";
    public static final String DIMENSION_SELECT = "select";
    public static final String DIMENSION_CORRECT = "correct";
    public static final String DIMENSION_MEMORY = "memory";
    public static final String DIMENSION_FALLBACK = "fallback";

    public static final String STATE_KEY_SOURCE_RESOLVED = "sourceResolved";
    public static final String STATE_KEY_SCHEMA_READY = "schemaReady";
    public static final String STATE_KEY_WRITE_OPERATION = "writeOperation";
    public static final String STATE_KEY_WRITE_CONFIRMED = "writeConfirmed";
    public static final String STATE_KEY_NEED_USER_QUESTION = "needUserQuestion";
    public static final String STATE_KEY_HAS_EXECUTION_ERROR = "hasExecutionError";
    public static final String STATE_KEY_CONFIDENCE = "confidence";
    public static final String STATE_KEY_RETRIES_USED = "retriesUsed";
    public static final String STATE_KEY_RETRY_BUDGET = "retryBudget";
    public static final String STATE_KEY_CANDIDATE_COUNT = "candidateCount";
    public static final String STATE_KEY_SELECTED_CANDIDATE_ID = "selectedCandidateId";
    public static final String STATE_KEY_AMBIGUITY_LEVEL = "ambiguityLevel";

    public static final int DEFAULT_RETRY_BUDGET = 2;
    public static final int DEFAULT_RETRIES_USED = 0;
    public static final int DEFAULT_CANDIDATE_COUNT = 0;
    public static final double DEFAULT_CONFIDENCE = 0.5D;
    public static final int SHORTLIST_MAX_HIGH = 3;
    public static final int SHORTLIST_MAX_DEFAULT = 2;

    public static final String SYMBOL_NEWLINE = "\n";
    public static final String SYMBOL_SEMICOLON = ";";
    public static final String SYMBOL_EQUAL = "=";
    public static final String SYMBOL_COLON = ":";

    public static final String TRIGGER_HINT_AMBIGU = "ambigu";
    public static final String TRIGGER_HINT_CN_SAME_NAME = "同名";
    public static final String TRIGGER_HINT_CN_CONFLICT = "冲突";
    public static final String TRIGGER_HINT_MULTI = "multi";
    public static final String TRIGGER_HINT_MULTI_CN = "多候选";
    public static final String TRIGGER_HINT_SINGLE = "single";
    public static final String TRIGGER_HINT_SINGLE_CN = "单候选";

    public static final int READINESS_DIMENSION_COUNT = 9;
}
