package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.DatabaseProvider;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.capability.SchemaProvider;
import edu.zsc.ai.plugin.driver.DriverLoader;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.mysql.connection.MysqlJdbcConnectionBuilder;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Abstract base class for MySQL database plugins.
 * Provides common functionality for different MySQL versions.
 * Implements ConnectionProvider, CommandExecutor, DatabaseProvider and SchemaProvider capabilities for all MySQL plugins.
 */
public abstract class AbstractMysqlPlugin extends AbstractDatabasePlugin
        implements ConnectionProvider, CommandExecutor<SqlCommandRequest, SqlCommandResult>, DatabaseProvider, SchemaProvider {

    private static final Logger logger = Logger.getLogger(AbstractMysqlPlugin.class.getName());

    /**
     * Connection builder - can be reused across all connection attempts
     */
    private final JdbcConnectionBuilder connectionBuilder = new MysqlJdbcConnectionBuilder();

    /**
     * SQL executor - can be reused for all SQL command executions
     */
    private final MySQLSqlExecutor sqlExecutor = new MySQLSqlExecutor();
    
    @Override
    public boolean supportSchema() {
        return false; // MySQL: DATABASE and SCHEMA are synonyms, no separate schema namespace
    }

    /**
     * Get MySQL-specific JDBC driver class name.
     * Different MySQL versions may use different driver classes.
     *
     * @return JDBC driver class name
     */
    protected abstract String getDriverClassName();
    
    /**
     * Get default JDBC URL template
     *
     * @return JDBC URL template
     */
    protected String getJdbcUrlTemplate() {
        return "jdbc:mysql://%s:%d/%s";
    }
    
    /**
     * Get default port for MySQL
     *
     * @return default port (3306)
     */
    protected int getDefaultPort() {
        return 3306;
    }

    // ========== ConnectionProvider Implementation ==========

    @Override
    public Connection connect(ConnectionConfig config) {
        try {
            // Step 1: Load JDBC driver
            DriverLoader.loadDriver(config, getDriverClassName());

            // Step 2: Build JDBC URL
            String jdbcUrl = connectionBuilder.buildUrl(config, getJdbcUrlTemplate(), getDefaultPort());

            // Step 3: Build connection properties
            Properties properties = connectionBuilder.buildProperties(config);

            // Step 4: Establish connection
            Connection connection = DriverManager.getConnection(jdbcUrl, properties);
            
            logger.info(String.format("Successfully connected to MySQL database at %s:%d/%s",
                config.getHost(), 
                config.getPort() != null ? config.getPort() : getDefaultPort(),
                config.getDatabase() != null ? config.getDatabase() : ""));
            
            return connection;

        } catch (SQLException e) {
            String errorMsg = String.format("Failed to connect to MySQL database at %s:%d/%s: %s",
                config.getHost(),
                config.getPort() != null ? config.getPort() : getDefaultPort(),
                config.getDatabase() != null ? config.getDatabase() : "",
                e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while connecting to MySQL database: %s", e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public boolean testConnection(ConnectionConfig config) {
        try {
            Connection connection = connect(config);
            if (connection != null && !connection.isClosed()) {
                closeConnection(connection);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warning(String.format("Connection test failed: %s", e.getMessage()));
            return false;
        }
    }

    @Override
    public void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to close database connection: " + e.getMessage(), e);
        }
    }

    // ========== CommandExecutor Implementation ==========

    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest command) {
        return sqlExecutor.executeCommand(command);
    }

    // ========== Driver Maven Coordinates ==========
    
    /**
     * Get Maven coordinates for MySQL JDBC driver.
     * Automatically selects the correct artifact based on version:
     * - MySQL 8.0+ (8.x, 9.x) → com.mysql:mysql-connector-j
     * - MySQL 5.x and below → mysql:mysql-connector-java
     *
     * @param driverVersion driver version (nullable, uses default if null)
     * @return Maven coordinates for MySQL driver
     * @throws IllegalArgumentException if version is not supported
     */
    @Override
    public MavenCoordinates getDriverMavenCoordinates(String driverVersion) {
        // If no version specified or version >= 8.0, use mysql-connector-j
        if (driverVersion == null || driverVersion.isEmpty() 
            || driverVersion.startsWith("8.") || driverVersion.startsWith("9.")) {
            String version = (driverVersion != null && !driverVersion.isEmpty()) ? driverVersion : "8.0.33";
            return new MavenCoordinates(
                "com.mysql",
                "mysql-connector-j",
                version
            );
        }
        
        // Version 2.x - 7.x → use mysql-connector-java
        char firstChar = driverVersion.charAt(0);
        if (firstChar >= '2' && firstChar <= '7') {
            return new MavenCoordinates(
                "mysql",
                "mysql-connector-java",
                driverVersion
            );
        }
        
        // Unsupported version - throw exception instead of returning null
        throw new IllegalArgumentException(
                String.format("Unsupported MySQL driver version: %s. Supported versions: 2.x-7.x, 8.x, 9.x", driverVersion));
    }
}
