package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.*;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.driver.DriverLoader;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.mysql.connection.MysqlJdbcConnectionBuilder;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class DefaultMysqlPlugin extends AbstractDatabasePlugin
        implements ConnectionProvider, CommandExecutor<SqlCommandRequest, SqlCommandResult>, DatabaseProvider,
        SchemaProvider, TableProvider {

    private static final Logger logger = Logger.getLogger(DefaultMysqlPlugin.class.getName());

    private final JdbcConnectionBuilder connectionBuilder = new MysqlJdbcConnectionBuilder();

    private final MySQLSqlExecutor sqlExecutor = new MySQLSqlExecutor();

    @Override
    public boolean supportSchema() {
        return false;
    }

    protected abstract String getDriverClassName();

    protected String getJdbcUrlTemplate() {
        return "jdbc:mysql://%s:%d/%s";
    }

    protected int getDefaultPort() {
        return 3306;
    }

    @Override
    public Connection connect(ConnectionConfig config) {
        try {
            DriverLoader.loadDriver(config, getDriverClassName());

            String jdbcUrl = connectionBuilder.buildUrl(config, getJdbcUrlTemplate(), getDefaultPort());

            Properties properties = connectionBuilder.buildProperties(config);

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

    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest command) {
        return sqlExecutor.executeCommand(command);
    }

    @Override
    public MavenCoordinates getDriverMavenCoordinates(String driverVersion) {
        if (driverVersion == null || driverVersion.isEmpty()
                || driverVersion.startsWith("8.") || driverVersion.startsWith("9.")) {
            String version = (driverVersion != null && !driverVersion.isEmpty()) ? driverVersion : "8.0.33";
            return new MavenCoordinates(
                    "com.mysql",
                    "mysql-connector-j",
                    version
            );
        }

        char firstChar = driverVersion.charAt(0);
        if (firstChar >= '2' && firstChar <= '7') {
            return new MavenCoordinates(
                    "mysql",
                    "mysql-connector-java",
                    driverVersion
            );
        }

        throw new IllegalArgumentException(
                String.format("Unsupported MySQL driver version: %s. Supported versions: 2.x-7.x, 8.x, 9.x", driverVersion));
    }

    @Override
    public String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        if (connection == null || tableName == null || tableName.isEmpty()) {
            return "";
        }

        String fullTableName = (catalog != null && !catalog.isEmpty()) 
                ? String.format("`%s`.`%s`", catalog, tableName) 
                : String.format("`%s`", tableName);
        String sql = String.format("SHOW CREATE TABLE %s", fullTableName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for table %s: %s",
                    fullTableName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get table DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get table DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        if (firstRow.size() < 2) {
            throw new RuntimeException("Failed to get table DDL: Unexpected result format");
        }
        return firstRow.get(1).toString();

    }
}
