package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capability for listing databases under the current data source.
 * Plugins that implement this can provide database list for a given connection,
 * so that UI or other clients can show and switch database.
 * <p>
 * Use {@link edu.zsc.ai.plugin.SqlPlugin#supportDatabase()} to decide whether to call
 * {@link #getDatabases(Connection)}.
 */
public interface DatabaseProvider {

    /**
     * List databases available on this data source for the given connection.
     * E.g. MySQL: SHOW DATABASES; PostgreSQL: databases in the cluster.
     *
     * @param connection the active connection (same data source)
     * @return list of database names, never null
     * @throws RuntimeException if listing fails (e.g. SQLException)
     */
    default List<String> getDatabases(Connection connection) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_CAT);
                    if (name != null && !name.isEmpty()) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list databases: " + e.getMessage(), e);
        }
    }
}
