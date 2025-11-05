package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.SqlPlugin;
import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.DriverLoader;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.ConnectionConfig;
import edu.zsc.ai.plugin.mysql.connection.MysqlJdbcConnectionBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Abstract base class for MySQL database plugins.
 * Provides common functionality for different MySQL versions.
 * Implements ConnectionProvider capability for all MySQL plugins.
 */
public abstract class AbstractMysqlPlugin extends AbstractDatabasePlugin implements SqlPlugin, ConnectionProvider {

    private static final Logger logger = Logger.getLogger(AbstractMysqlPlugin.class.getName());

    /**
     * Connection builder - can be reused across all connection attempts
     */
    private final JdbcConnectionBuilder connectionBuilder = new MysqlJdbcConnectionBuilder();
    
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
    public Connection connect(ConnectionConfig config) throws PluginException {
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
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch (PluginException e) {
            // Re-throw PluginException as-is
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while connecting to MySQL database: %s", e.getMessage());
            logger.severe(errorMsg);
            throw new PluginException(PluginErrorCode.CONNECTION_FAILED, errorMsg, e);
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
    public void closeConnection(Connection connection) throws PluginException {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            throw new PluginException("Failed to close database connection: " + e.getMessage(), e);
        }
    }
}
