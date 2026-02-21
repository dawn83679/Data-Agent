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
     * Column default value in ResultSet from getColumns.
     */
    public static final String COLUMN_DEF = "COLUMN_DEF";

    /**
     * Table type constant for base tables.
     */
    public static final String TABLE_TYPE_TABLE = "TABLE";

    /**
     * Table type constant for views.
     */
    public static final String TABLE_TYPE_VIEW = "VIEW";

    // --- getIndexInfo ---
    /** Index name in ResultSet from getIndexInfo. */
    public static final String INDEX_NAME = "INDEX_NAME";
    /** Non-unique flag in ResultSet from getIndexInfo. */
    public static final String NON_UNIQUE = "NON_UNIQUE";
    /** Index type: tableIndexStatistic, tableIndexClustered, etc. */
    public static final String TYPE = "TYPE";
    /** Ascending or descending in getIndexInfo. */
    public static final String ASC_OR_DESC = "ASC_OR_DESC";

    // --- getProcedures ---
    /** Procedure catalog in ResultSet from getProcedures. */
    public static final String PROCEDURE_CAT = "PROCEDURE_CAT";
    /** Procedure schema in ResultSet from getProcedures. */
    public static final String PROCEDURE_SCHEM = "PROCEDURE_SCHEM";
    /** Procedure name in ResultSet from getProcedures. */
    public static final String PROCEDURE_NAME = "PROCEDURE_NAME";
    /** Procedure type in ResultSet from getProcedures. */
    public static final String PROCEDURE_TYPE = "PROCEDURE_TYPE";

    // --- getFunctions ---
    /** Function catalog in ResultSet from getFunctions. */
    public static final String FUNCTION_CAT = "FUNCTION_CAT";
    /** Function schema in ResultSet from getFunctions. */
    public static final String FUNCTION_SCHEM = "FUNCTION_SCHEM";
    /** Function name in ResultSet from getFunctions. */
    public static final String FUNCTION_NAME = "FUNCTION_NAME";
    /** Function type in ResultSet from getFunctions. */
    public static final String FUNCTION_TYPE = "FUNCTION_TYPE";

    private JdbcMetaDataConstants() {
    }
}
