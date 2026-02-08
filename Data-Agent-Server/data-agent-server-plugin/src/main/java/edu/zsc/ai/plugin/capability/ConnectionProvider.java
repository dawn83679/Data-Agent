package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.connection.ConnectionConfig;
import java.sql.*;

/**
 * Connection provider capability interface.
 * Plugins implementing this interface can establish and manage database connections.
 */
public interface ConnectionProvider {
    
    /**
     * Establish a database connection based on the provided configuration.
     */
    Connection connect(ConnectionConfig config);
    
    /**
     * Test whether a connection can be established with the given configuration.
     */
    boolean testConnection(ConnectionConfig config);
    
    /**
     * Close a database connection and release associated resources.
     */
    void closeConnection(Connection connection);

    /**
     * Get database metadata from a connection.
     */
    default DatabaseMetaData getMetaData(Connection connection) {
        try {
            return connection.getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Get database product version from a connection.
     */
    default String getDatabaseProductVersion(Connection connection) {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database product version: " + e.getMessage(), e);
        }
    }

    /**
     * Get formatted database information string.
     */
    default String getDbmsInfo(Connection connection) {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return String.format("%s (ver. %s)",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database info: " + e.getMessage(), e);
        }
    }

    /**
     * Get formatted driver information string.
     */
    default String getDriverInfo(Connection connection) {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return String.format("%s (ver. %s, JDBC%d.%d)",
                    metaData.getDriverName(),
                    metaData.getDriverVersion(),
                    metaData.getJDBCMajorVersion(),
                    metaData.getJDBCMinorVersion());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get driver info: " + e.getMessage(), e);
        }
    }
}

