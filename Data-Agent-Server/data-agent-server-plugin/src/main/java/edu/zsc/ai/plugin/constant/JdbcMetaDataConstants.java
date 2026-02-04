package edu.zsc.ai.plugin.constant;

/**
 * JDBC metadata ResultSet column name constants.
 * Used when reading from {@link java.sql.DatabaseMetaData} result sets (e.g. getCatalogs, getSchemas).
 */
public final class JdbcMetaDataConstants {

    /**
     * Catalog column name in ResultSet from {@link java.sql.DatabaseMetaData#getCatalogs()}.
     */
    public static final String TABLE_CAT = "TABLE_CAT";

    /**
     * Schema column name in ResultSet from {@link java.sql.DatabaseMetaData#getSchemas(String, String)}.
     */
    public static final String TABLE_SCHEM = "TABLE_SCHEM";

    /**
     * Table name column name in ResultSet from {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])}.
     */
    public static final String TABLE_NAME = "TABLE_NAME";

    /**
     * Column name in ResultSet from {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}.
     */
    public static final String COLUMN_NAME = "COLUMN_NAME";

    /**
     * Data type (java.sql.Types) in ResultSet from getColumns.
     */
    public static final String DATA_TYPE = "DATA_TYPE";

    /**
     * Type name in ResultSet from getColumns.
     */
    public static final String TYPE_NAME = "TYPE_NAME";

    /**
     * Column size in ResultSet from getColumns.
     */
    public static final String COLUMN_SIZE = "COLUMN_SIZE";

    /**
     * Decimal digits in ResultSet from getColumns.
     */
    public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";

    /**
     * Nullable indicator in ResultSet from getColumns.
     */
    public static final String NULLABLE = "NULLABLE";

    /**
     * Ordinal position in ResultSet from getColumns.
     */
    public static final String ORDINAL_POSITION = "ORDINAL_POSITION";

    /**
     * Remarks/comments in ResultSet from getColumns.
     */
    public static final String REMARKS = "REMARKS";

    /**
     * Table type constant for base tables.
     */
    public static final String TABLE_TYPE_TABLE = "TABLE";

    /**
     * Table type constant for views.
     */
    public static final String TABLE_TYPE_VIEW = "VIEW";

    private JdbcMetaDataConstants() {
    }
}
