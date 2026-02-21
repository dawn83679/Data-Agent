package edu.zsc.ai.plugin.mysql.constant;

import static edu.zsc.ai.plugin.mysql.constant.MysqlRoutineConstants.*;
import static edu.zsc.ai.plugin.mysql.constant.MysqlTriggerConstants.*;

/**
 * All SQL strings used by MySQL plugin.
 */
public final class MysqlSqlConstants {

    // --- SHOW commands ---
    /** %s = full table name (catalog.table or table) */
    public static final String SQL_SHOW_CREATE_TABLE = "SHOW CREATE TABLE %s";
    /** %s = full view name (catalog.view or view) */
    public static final String SQL_SHOW_CREATE_VIEW = "SHOW CREATE VIEW %s";
    /** %s = full function name (catalog.func or func) */
    public static final String SQL_SHOW_CREATE_FUNCTION = "SHOW CREATE FUNCTION %s";
    /** %s = full procedure name (catalog.proc or proc) */
    public static final String SQL_SHOW_CREATE_PROCEDURE = "SHOW CREATE PROCEDURE %s";
    /** %s = full trigger name (catalog.trigger or trigger) */
    public static final String SQL_SHOW_CREATE_TRIGGER = "SHOW CREATE TRIGGER %s";

    // --- information_schema.TRIGGERS ---
    /** %s = escaped schema. Append SQL_TRIGGER_FILTER_BY_TABLE + escapedTable + "'" for table filter. */
    public static final String SQL_LIST_TRIGGERS =
            "SELECT " + TRIGGER_NAME + ", " + EVENT_OBJECT_TABLE + ", " + ACTION_TIMING + ", " + EVENT_MANIPULATION
                    + " FROM information_schema.TRIGGERS"
                    + " WHERE " + TRIGGER_SCHEMA + " = '%s'";
    public static final String SQL_TRIGGER_FILTER_BY_TABLE = " AND " + EVENT_OBJECT_TABLE + " = '";

    // --- information_schema.ROUTINES ---
    /** %s = escaped schema */
    public static final String SQL_LIST_FUNCTIONS =
            "SELECT " + SPECIFIC_NAME + ", " + ROUTINE_NAME + ", " + DTD_IDENTIFIER
                    + " FROM information_schema.ROUTINES"
                    + " WHERE " + ROUTINE_SCHEMA + " = '%s'"
                    + " AND " + ROUTINE_TYPE + " = '" + ROUTINE_TYPE_FUNCTION + "'";
    /** %s = escaped schema */
    public static final String SQL_LIST_PROCEDURES =
            "SELECT " + SPECIFIC_NAME + ", " + ROUTINE_NAME
                    + " FROM information_schema.ROUTINES"
                    + " WHERE " + ROUTINE_SCHEMA + " = '%s'"
                    + " AND " + ROUTINE_TYPE + " = '" + ROUTINE_TYPE_PROCEDURE + "'";

    // --- information_schema.COLUMNS ---
    /** %s = TABLE_SCHEMA, %s = TABLE_NAME. For tables and views. */
    public static final String SQL_LIST_COLUMNS =
            "SELECT " + MysqlColumnConstants.COLUMN_NAME + ", " + MysqlColumnConstants.ORDINAL_POSITION
                    + ", " + MysqlColumnConstants.COLUMN_DEFAULT + ", " + MysqlColumnConstants.IS_NULLABLE
                    + ", " + MysqlColumnConstants.DATA_TYPE + ", " + MysqlColumnConstants.COLUMN_TYPE
                    + ", " + MysqlColumnConstants.COLUMN_KEY + ", " + MysqlColumnConstants.EXTRA
                    + ", " + MysqlColumnConstants.COLUMN_COMMENT
                    + ", " + MysqlColumnConstants.CHARACTER_MAXIMUM_LENGTH
                    + ", " + MysqlColumnConstants.NUMERIC_PRECISION + ", " + MysqlColumnConstants.NUMERIC_SCALE
                    + " FROM information_schema.COLUMNS"
                    + " WHERE " + MysqlColumnConstants.TABLE_SCHEMA + " = '%s'"
                    + " AND " + MysqlColumnConstants.TABLE_NAME + " = '%s'"
                    + " ORDER BY " + MysqlColumnConstants.ORDINAL_POSITION;

    // --- information_schema.PARAMETERS ---
    /** %s = escaped schema, %s = IN clause (e.g. 'fn1','fn2') */
    public static final String SQL_FETCH_PARAMETERS =
            "SELECT " + SPECIFIC_NAME + ", " + PARAMETER_NAME + ", " + DTD_IDENTIFIER + ", " + ORDINAL_POSITION
                    + " FROM information_schema.PARAMETERS"
                    + " WHERE " + SPECIFIC_SCHEMA + " = '%s'"
                    + " AND " + SPECIFIC_NAME + " IN (%s)"
                    + " AND " + ORDINAL_POSITION + " > 0"
                    + " AND " + PARAMETER_NAME + " IS NOT NULL";

    private MysqlSqlConstants() {
    }
}
