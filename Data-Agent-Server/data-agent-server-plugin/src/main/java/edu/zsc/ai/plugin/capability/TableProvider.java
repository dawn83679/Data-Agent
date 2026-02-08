package edu.zsc.ai.plugin.capability;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

/**
 * Capability for listing tables and retrieving table DDL.
 * Plugins that implement this can provide table metadata for schema browsing, SQL editing, etc.
 */
public interface TableProvider {

    /**
     * List base tables in the given catalog/schema (excludes views).
     *
     * @param connection the active connection
     * @param catalog    catalog/database name; may be null for current catalog
     * @param schema     schema name; may be null
     * @return list of table names, never null
     */
    default List<String> getTableNames(Connection connection, String catalog, String schema) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables(
                    catalog, schema, null, new String[] {JdbcMetaDataConstants.TABLE_TYPE_TABLE})) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_NAME);
                    if (name != null && !name.isEmpty()) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tables: " + e.getMessage(), e);
        }
    }

    /**
     * Get DDL for the specified table.
     *
     * @param connection the active connection
     * @param catalog    catalog/database name; may be null
     * @param schema     schema name; may be null
     * @param tableName  table name
     * @return table DDL statement
     */
    default String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support getting table DDL");
    }
}
