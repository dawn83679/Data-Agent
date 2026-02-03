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

    private JdbcMetaDataConstants() {
    }
}
