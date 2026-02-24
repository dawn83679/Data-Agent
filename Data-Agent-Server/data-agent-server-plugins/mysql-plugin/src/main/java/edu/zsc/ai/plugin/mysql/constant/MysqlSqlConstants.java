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
    /** %s = escaped schema. Append SQL_TRIGGER_FILTER_BY_TABLE + escapedTable + "" for table filter. */
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

    // --- Table/View Data Query (with pagination) ---
    /** %1$s = table/view name, %2$s = offset, %3$s = page size */
    public static final String SQL_SELECT_TABLE_DATA =
            "SELECT * FROM %s LIMIT %d OFFSET %d";

    /** %1$s = table/view name */
    public static final String SQL_COUNT_TABLE_DATA =
            "SELECT COUNT(*) AS total FROM %s";

    // --- Database Export/Import ---
    /** %1$s = database name */
    public static final String SQL_SHOW_CREATE_DATABASE =
            "SHOW CREATE DATABASE `%s`";

    // --- Character Sets and Collations ---
    /** Show all character sets */
    public static final String SQL_SHOW_CHARACTER_SET = "SHOW CHARACTER SET";

    /** %s = character set name */
    public static final String SQL_SHOW_COLLATION = "SHOW COLLATION WHERE Charset = '%s'";

    // --- Database Operations ---
    /** %1$s = database name */
    public static final String SQL_SHOW_DATABASES_LIKE = "SHOW DATABASES LIKE '%s'";

    // --- Table Operations ---
    /** Show all storage engines */
    public static final String SQL_SHOW_ENGINES = "SHOW ENGINES";

    // --- Table Drop ---
    /** %s = table name */
    public static final String SQL_DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS `%s`";

    // --- Database Drop ---
    /** %s = database name */
    public static final String SQL_DROP_DATABASE = "DROP DATABASE `%s`";

    // --- Database Create ---
    /** %1$s = database name, %2$s = charset (may be empty), %3$s = collation (may be empty) */
    public static final String SQL_CREATE_DATABASE = "CREATE DATABASE `%s`%s%s";

    // --- Routine (Procedure/Function) Create ---
    /** %1$s = schema prefix, %2$s = routine name, %3$s = parameters, %4$s = body */
    public static final String SQL_CREATE_PROCEDURE = "CREATE PROCEDURE %s`%s`%s %s";

    /** %1$s = schema prefix, %2$s = routine name, %3$s = parameters, %4$s = return type, %5$s = body */
    public static final String SQL_CREATE_FUNCTION = "CREATE FUNCTION %s`%s`%s RETURNS %s %s";

    // --- Routine Drop ---
    /** %1$s = schema prefix, %2$s = procedure name */
    public static final String SQL_DROP_PROCEDURE = "DROP PROCEDURE %s`%s`";

    /** %1$s = schema prefix, %2$s = function name */
    public static final String SQL_DROP_FUNCTION = "DROP FUNCTION %s`%s`";

    // --- Trigger Create ---
    /** %1$s = trigger name, %2$s = table name, %3$s = timing, %4$s = event, %5$s = body */
    public static final String SQL_CREATE_TRIGGER = "CREATE TRIGGER `%s` %s %s ON `%s` FOR EACH ROW %s";

    // --- Trigger Drop ---
    /** %1$s = schema prefix, %2$s = trigger name */
    public static final String SQL_DROP_TRIGGER = "DROP TRIGGER %s`%s`";

    // --- View Create ---
    /** %1$s = view name, %2$s = query */
    public static final String SQL_CREATE_VIEW = "CREATE VIEW `%s` AS %s";

    // --- View Drop ---
    /** %s = view name */
    public static final String SQL_DROP_VIEW = "DROP VIEW IF EXISTS `%s`";

    // --- Table Data Modification ---
    /** %1$s = table name, %2$s = columns (comma separated), %3$s = values (comma separated) */
    public static final String SQL_INSERT_INTO = "INSERT INTO `%s` (%s) VALUES (%s)";

    /** %1$s = table name, %2$s = set clause, %3$s = where clause */
    public static final String SQL_UPDATE = "UPDATE `%s` SET %s WHERE %s";

    /** %1$s = table name, %2$s = where clause */
    public static final String SQL_DELETE_FROM = "DELETE FROM `%s` WHERE %s";

    private MysqlSqlConstants() {
    }
}
