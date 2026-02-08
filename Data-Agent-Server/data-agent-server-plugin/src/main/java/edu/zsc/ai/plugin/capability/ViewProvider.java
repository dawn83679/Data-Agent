package edu.zsc.ai.plugin.capability;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

/**
 * Capability for listing views and retrieving view DDL.
 * Plugins that implement this can provide view metadata for schema browsing, SQL editing, etc.
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
     * Get DDL for the specified view.
     */
    String getViewDdl(Connection connection, String catalog, String schema, String viewName);
}
