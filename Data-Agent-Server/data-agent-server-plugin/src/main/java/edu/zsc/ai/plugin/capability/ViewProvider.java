package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.SqlPlugin;
import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability for listing views under a catalog/schema.
 * Plugins that implement this can provide view list for a given connection and
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
public interface ViewProvider {

    /**
     * List views in the given catalog/schema.
     */
    default List<String> getViews(Connection connection, String catalog, String schema) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables(
                    catalog, schema, null, new String[] { JdbcMetaDataConstants.TABLE_TYPE_VIEW })) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_NAME);
                    if (name != null && !name.isEmpty()) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list views: " + e.getMessage(), e);
        }
    }

    /**
     * Get DDL statement for the specified view.
     */
    String getViewDdl(Connection connection, String catalog, String schema, String viewName);
}
