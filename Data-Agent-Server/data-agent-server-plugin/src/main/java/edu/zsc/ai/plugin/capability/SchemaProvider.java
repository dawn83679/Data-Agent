package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.SqlPlugin;
import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability for listing schemas under the current data source (within a database/catalog).
 * Plugins that implement this can provide schema list for a given connection and catalog,
 * so that UI or other clients can show and switch schema.
 * <p>
 * Use {@link SqlPlugin#supportSchema()} to decide whether to call
 * {@link #getSchemas(Connection, String)}.
 */
public interface SchemaProvider {

    /**
     * List schemas in the given catalog for the given connection.
     */
    default List<String> getSchemas(Connection connection, String catalog) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getSchemas(catalog, null)) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_SCHEM);
                    if (name != null && !name.isEmpty()) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list schemas: " + e.getMessage(), e);
        }
    }
}
