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
     *
     * @param config connection configuration
     * @return database connection
     * @throws RuntimeException if connection fails
     */
    Connection connect(ConnectionConfig config);
    
    /**
     * Test whether a connection can be established with the given configuration.
     * This method should not throw exceptions, but return false on failure.
     *
     * @param config connection configuration
     * @return true if connection test succeeds, false otherwise
     */
    boolean testConnection(ConnectionConfig config);
    
    /**
     * Close a database connection and release associated resources.
     *
     * @param connection the connection to close
     * @throws RuntimeException if closing the connection fails
     */
    void closeConnection(Connection connection);

    /**
     * Get database metadata from a connection.
     * Default implementation using JDBC standard method.
     *
     * @param connection the database connection
     * @return DatabaseMetaData instance
     * @throws RuntimeException if getting metadata fails
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
     * Default implementation using JDBC standard method via getMetaData().
     *
     * @param connection the database connection
     * @return database product version string
     * @throws RuntimeException if getting version fails
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
     * Default implementation using JDBC standard methods.
     *
     * @param connection the database connection
     * @return formatted database info string (e.g., "MySQL (ver. 8.0.0)")
     * @throws RuntimeException if getting info fails
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
     * Default implementation using JDBC standard methods.
     *
     * @param connection the database connection
     * @return formatted driver info string (e.g., "MySQL Connector/J (ver. 8.0.0, JDBC4.2)")
     * @throws RuntimeException if getting info fails
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

