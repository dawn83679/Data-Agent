package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.driver.DriverLoader;
import edu.zsc.ai.plugin.mysql.util.MysqlJdbcConnectionBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class MysqlConnectionManager implements ConnectionManager {

    private static final Logger logger = Logger.getLogger(MysqlConnectionManager.class.getName());

    private final JdbcConnectionBuilder connectionBuilder = new MysqlJdbcConnectionBuilder();
    private final Supplier<String> driverClassNameSupplier;
    private final Supplier<String> jdbcUrlTemplateSupplier;
    private final IntSupplier defaultPortSupplier;

    public MysqlConnectionManager(Supplier<String> driverClassNameSupplier,
                                  Supplier<String> jdbcUrlTemplateSupplier,
                                  IntSupplier defaultPortSupplier) {
        this.driverClassNameSupplier = driverClassNameSupplier;
        this.jdbcUrlTemplateSupplier = jdbcUrlTemplateSupplier;
        this.defaultPortSupplier = defaultPortSupplier;
    }

    @Override
    public Connection connect(ConnectionConfig config) {
        try {
            DriverLoader.loadDriver(config, driverClassNameSupplier.get());

            String jdbcUrl = connectionBuilder.buildUrl(
                    config,
                    jdbcUrlTemplateSupplier.get(),
                    defaultPortSupplier.getAsInt()
            );
            Properties properties = connectionBuilder.buildProperties(config);

            Connection connection = DriverManager.getConnection(jdbcUrl, properties);
            logger.info(String.format(
                    "Successfully connected to MySQL database at %s:%d/%s",
                    config.getHost(),
                    config.getPort() != null ? config.getPort() : defaultPortSupplier.getAsInt(),
                    config.getDatabase() != null ? config.getDatabase() : ""
            ));
            return connection;
        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Failed to connect to MySQL database at %s:%d/%s: %s",
                    config.getHost(),
                    config.getPort() != null ? config.getPort() : defaultPortSupplier.getAsInt(),
                    config.getDatabase() != null ? config.getDatabase() : "",
                    e.getMessage()
            );
            logger.severe(errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format(
                    "Unexpected error while connecting to MySQL database: %s",
                    e.getMessage()
            );
            logger.severe(errorMessage);
            throw new RuntimeException(errorMessage, e);
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
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database connection: " + e.getMessage(), e);
        }
    }
}
