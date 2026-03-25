package edu.zsc.ai.common.constant;

public final class CompressionLogConstant {

    public static final String LOGGER_NAME = "ChatMemoryCompressor";

    public static final String EVENT_COMPRESSION_STARTED = "compression_started";
    public static final String EVENT_COMPRESSION_COMPLETED = "compression_completed";
    public static final String EVENT_COMPRESSION_FAILED = "compression_failed";
    public static final String EVENT_COMPRESSION_SKIPPED = "compression_skipped";

    public static final String FIELD_DECISION = "decision";
    public static final String FIELD_MODEL_NAME = "modelName";
    public static final String FIELD_TOKEN_COUNT_BEFORE = "tokenCountBefore";
    public static final String FIELD_TOKEN_COUNT_AFTER = "tokenCountAfter";
    public static final String FIELD_THRESHOLD = "threshold";
    public static final String FIELD_MESSAGE_COUNT = "messageCount";
    public static final String FIELD_COMPRESSED_MESSAGE_COUNT = "compressedMessageCount";
    public static final String FIELD_KEPT_RECENT_COUNT = "keptRecentCount";
    public static final String FIELD_SUMMARY_LENGTH = "summaryLength";
    public static final String FIELD_OUTPUT_TOKENS = "outputTokens";
    public static final String FIELD_TOTAL_TOKENS = "totalTokens";

    public static final String DECISION_COMPRESS = "compress";
    public static final String DECISION_COMPRESSED = "compressed";
    public static final String DECISION_SKIP_NOT_ENOUGH_MESSAGES = "skip_not_enough_messages";
    public static final String DECISION_SKIP_ALREADY_IN_PROGRESS = "skip_already_in_progress";

    private CompressionLogConstant() {
    }
}
