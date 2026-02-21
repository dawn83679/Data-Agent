package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.connection.ConnectionConfig;
import java.sql.*;

public interface ConnectionProvider {

    Connection connect(ConnectionConfig config);

    boolean testConnection(ConnectionConfig config);

    void closeConnection(Connection connection);

    default DatabaseMetaData getMetaData(Connection connection) {
        try {
            return connection.getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database metadata: " + e.getMessage(), e);
        }
    }

    default String getDatabaseProductVersion(Connection connection) {
        try {
            DatabaseMetaData metaData = getMetaData(connection);
            return metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database product version: " + e.getMessage(), e);
        }
    }

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
