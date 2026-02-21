package edu.zsc.ai.plugin.mysql.constant;

/**
 * Column names for MySQL information_schema.TRIGGERS result sets.
 */
public final class MysqlTriggerConstants {

    public static final String TRIGGER_SCHEMA = "TRIGGER_SCHEMA";
    public static final String TRIGGER_NAME = "TRIGGER_NAME";
    public static final String EVENT_MANIPULATION = "EVENT_MANIPULATION";
    public static final String EVENT_OBJECT_TABLE = "EVENT_OBJECT_TABLE";
    public static final String ACTION_TIMING = "ACTION_TIMING";

    private MysqlTriggerConstants() {
    }
}
