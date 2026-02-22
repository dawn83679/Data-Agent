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

public interface TableProvider {

    default List<String> getTableNames(Connection connection, String catalog, String schema) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables(
                    catalog, schema, null, new String[] {JdbcMetaDataConstants.TABLE_TYPE_TABLE})) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_NAME);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list tables: " + e.getMessage(), e);
        }
    }

    default String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support getting table DDL");
    }

    default void deleteTable(Connection connection, String pluginId, String catalog, String schema, String tableName) {
        Logger log = LoggerFactory.getLogger(TableProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = buildDropTableSql(schema, tableName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete table: " + result.getErrorMessage());
        }

        log.info("Table deleted successfully: catalog={}, schema={}, tableName={}", catalog, schema, tableName);
    }

    default String buildDropTableSql(String schema, String tableName) {
        StringBuilder sql = new StringBuilder("DROP TABLE ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(schema).append("`.");
        }
        sql.append("`").append(tableName).append("`");
        return sql.toString();
    }

    /**
     * Get table data with pagination
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param tableName table name
     * @param offset offset for pagination
     * @param pageSize page size
     * @return table data as ResultSet (caller must close it)
     */
    default ResultSet getTableData(Connection connection, String catalog, String schema, String tableName, int offset, int pageSize) {
        throw new UnsupportedOperationException("Plugin does not support getting table data");
    }

    /**
     * Get total count of table rows
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param tableName table name
     * @return total row count
     */
    default long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support getting table data count");
    }
}
