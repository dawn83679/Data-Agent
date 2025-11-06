package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.annotation.CapabilityMarker;
import edu.zsc.ai.plugin.enums.CapabilityEnum;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.ConnectionConfig;
import java.sql.*;

/**
 * Connection provider capability interface.
 * Plugins implementing this interface can establish and manage database connections.
 */
@CapabilityMarker(CapabilityEnum.CONNECTION)
public interface ConnectionProvider {
    
    /**
     * Establish a database connection based on the provided configuration.
     *
     * @param config connection configuration
     * @return database connection
     * @throws PluginException if connection fails
     */
    Connection connect(ConnectionConfig config) throws PluginException;
    
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
     * @throws PluginException if closing the connection fails
     */
    void closeConnection(Connection connection) throws PluginException;

    /**
     * Get database metadata from a connection.
     * Default implementation using JDBC standard method.
     *
     * @param connection the database connection
     * @return DatabaseMetaData instance
     * @throws PluginException if getting metadata fails
     */
    default DatabaseMetaData getMetaData(Connection connection) throws PluginException {
        try {
            return connection.getMetaData();
        } catch (SQLException e) {
            throw new PluginException("Failed to get database metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Get database product version from a connection.
     * Default implementation using JDBC standard method via getMetaData().
     *
     * @param connection the database connection
     * @return database product version string
     * @throws PluginException if getting version fails
     */
    default String getDatabaseProductVersion(Connection connection) throws PluginException {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new PluginException("Failed to get database product version: " + e.getMessage(), e);
        }
    }

    /**
     * Get formatted database information string.
     * Default implementation using JDBC standard methods.
     *
     * @param connection the database connection
     * @return formatted database info string (e.g., "MySQL (ver. 8.0.0)")
     * @throws PluginException if getting info fails
     */
    default String getDbmsInfo(Connection connection) throws PluginException {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return String.format("%s (ver. %s)",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new PluginException("Failed to get database info: " + e.getMessage(), e);
        }
    }

    /**
     * Get formatted driver information string.
     * Default implementation using JDBC standard methods.
     *
     * @param connection the database connection
     * @return formatted driver info string (e.g., "MySQL Connector/J (ver. 8.0.0, JDBC4.2)")
     * @throws PluginException if getting info fails
     */
    default String getDriverInfo(Connection connection) throws PluginException {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return String.format("%s (ver. %s, JDBC%d.%d)",
                    metaData.getDriverName(),
                    metaData.getDriverVersion(),
                    metaData.getJDBCMajorVersion(),
                    metaData.getJDBCMinorVersion());
        } catch (SQLException e) {
            throw new PluginException("Failed to get driver info: " + e.getMessage(), e);
        }
    }
}

