package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface DatabaseProvider {

    default List<String> getDatabases(Connection connection) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_CAT);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list databases: " + e.getMessage(), e);
        }
    }

    default void deleteDatabase(Connection connection, String pluginId, String databaseName) {
        Logger log = LoggerFactory.getLogger(DatabaseProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = String.format("DROP DATABASE `%s`", databaseName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(databaseName);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete database: " + result.getErrorMessage());
        }

        log.info("Database deleted successfully: databaseName={}", databaseName);
    }

    /**
     * Export database DDL (CREATE DATABASE statement)
     * @param connection database connection
     * @param databaseName database name
     * @return CREATE DATABASE statement
     */
    default String exportDatabaseDdl(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support exporting database DDL");
    }

    /**
     * Export all table DDLs in a database
     * @param connection database connection
     * @param databaseName database name
     * @return list of CREATE TABLE statements
     */
    default List<String> exportAllTableDdls(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support exporting table DDLs");
    }

    /**
     * Execute SQL script for import
     * @param connection database connection
     * @param sqlScript SQL script to execute
     * @return success or not
     */
    default void executeSqlScript(Connection connection, String sqlScript) {
        throw new UnsupportedOperationException("Plugin does not support executing SQL script");
    }

    /**
     * Get list of available character sets for creating database
     * @param connection database connection
     * @return list of character set names
     */
    default List<String> getCharacterSets(Connection connection) {
        throw new UnsupportedOperationException("Plugin does not support getting character sets");
    }

    /**
     * Get list of available collations for a given character set
     * @param connection database connection
     * @param charset character set name
     * @return list of collation names
     */
    default List<String> getCollations(Connection connection, String charset) {
        throw new UnsupportedOperationException("Plugin does not support getting collations");
    }

    /**
     * Create a new database
     * @param connection database connection
     * @param databaseName database name
     * @param charset character set
     * @param collation collation (sorting rule)
     */
    default void createDatabase(Connection connection, String databaseName, String charset, String collation) {
        throw new UnsupportedOperationException("Plugin does not support creating database");
    }

    /**
     * Check if a database exists
     * @param connection database connection
     * @param databaseName database name to check
     * @return true if database exists
     */
    default boolean databaseExists(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support checking database existence");
    }
}
