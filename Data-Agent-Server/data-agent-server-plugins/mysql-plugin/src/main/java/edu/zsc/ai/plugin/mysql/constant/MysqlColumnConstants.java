package edu.zsc.ai.plugin.mysql.constant;

/**
 * Column names for MySQL information_schema.COLUMNS result set.
 * Used to fetch rich column metadata (unsigned, auto_increment, etc.) not available from JDBC getColumns.
 */
public final class MysqlColumnConstants {

    public static final String TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String TABLE_NAME = "TABLE_NAME";
    public static final String COLUMN_NAME = "COLUMN_NAME";
    public static final String ORDINAL_POSITION = "ORDINAL_POSITION";
    public static final String COLUMN_DEFAULT = "COLUMN_DEFAULT";
    public static final String IS_NULLABLE = "IS_NULLABLE";
    public static final String DATA_TYPE = "DATA_TYPE";
    public static final String COLUMN_TYPE = "COLUMN_TYPE";
    public static final String COLUMN_KEY = "COLUMN_KEY";
    public static final String EXTRA = "EXTRA";
    public static final String COLUMN_COMMENT = "COLUMN_COMMENT";
    public static final String CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String NUMERIC_PRECISION = "NUMERIC_PRECISION";
    public static final String NUMERIC_SCALE = "NUMERIC_SCALE";

    /** COLUMN_KEY value for primary key. */
    public static final String COLUMN_KEY_PRI = "PRI";
    /** EXTRA value substring for auto_increment. */
    public static final String EXTRA_AUTO_INCREMENT = "auto_increment";

    private MysqlColumnConstants() {
    }
}
