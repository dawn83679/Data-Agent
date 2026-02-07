package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.SqlPlugin;
import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability for listing tables (base tables only, excluding views) under a
 * catalog/schema.
 * Plugins that implement this can provide table list for a given connection and
 * scope.
 * <p>
 * Use {@link SqlPlugin#supportDatabase()} /
 * {@link SqlPlugin#supportSchema()}
 * to decide catalog/schema semantics:
 * <ul>
 * <li>MySQL: catalog = database name, schema = null or same as catalog</li>
 * <li>PostgreSQL: catalog may be null, schema = namespace</li>
 * </ul>
 */
public interface TableProvider {

    /**
     * List base tables in the given catalog/schema.
     * Excludes views (use {@link ViewProvider} for views).
     *
     * @param connection the active connection
     * @param catalog    catalog/database name; may be null for current catalog
     * @param schema     schema name; may be null
     * @return list of table names, never null
     * @throws RuntimeException if listing fails
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
     * Get DDL statement for the specified table.
     * Default implementation throws UnsupportedOperationException; plugins should
     * override if supported.
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
