package edu.zsc.ai.plugin.mysql.constant;

/**
 * Column names for MySQL SHOW CREATE TABLE / SHOW CREATE VIEW result sets.
 *
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/show-create-table.html">SHOW CREATE TABLE</a>
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/show-create-view.html">SHOW CREATE VIEW</a>
 */
public final class MysqlShowColumnConstants {

    /** Column "Table" in SHOW CREATE TABLE result. */
    public static final String TABLE = "Table";
    /** Column "Create Table" in SHOW CREATE TABLE result (DDL). */
    public static final String CREATE_TABLE = "Create Table";
    /** Column "View" in SHOW CREATE VIEW result. */
    public static final String VIEW = "View";
    /** Column "Create View" in SHOW CREATE VIEW result (DDL). */
    public static final String CREATE_VIEW = "Create View";
    /** Column "Create Function" in SHOW CREATE FUNCTION result (DDL). */
    public static final String CREATE_FUNCTION = "Create Function";
    /** Column "Create Procedure" in SHOW CREATE PROCEDURE result (DDL). */
    public static final String CREATE_PROCEDURE = "Create Procedure";
    /** Column "SQL Original Statement" in SHOW CREATE TRIGGER result (DDL). */
    public static final String SQL_ORIGINAL_STATEMENT = "SQL Original Statement";

    private MysqlShowColumnConstants() {
    }
}
