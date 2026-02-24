package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

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

    /**
     * Insert data into table
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param tableName table name
     * @param columns column names
     * @param valuesList list of value maps (each map represents a row)
     * @return number of affected rows
     */
    default int insertTableData(Connection connection, String catalog, String schema, String tableName,
                            List<String> columns, List<Map<String, Object>> valuesList) {
        Logger log = LoggerFactory.getLogger(TableProvider.class);

        try {
            // Build column list
            StringJoiner columnJoiner = new StringJoiner(", ");
            for (String column : columns) {
                columnJoiner.add("`" + column + "`");
            }
            String columnList = columnJoiner.toString();

            // Build SQL using constant
            String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
            String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", tableName, columnList, placeholders);

            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (Map<String, Object> row : valuesList) {
                    ps.clearParameters();
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, row.get(columns.get(i)));
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            log.info("Data inserted successfully: tableName={}", tableName);
            return valuesList.size();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert data: " + e.getMessage(), e);
        }
    }

    /**
     * Update data in table
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param tableName table name
     * @param values column values to update
     * @param whereConditions WHERE conditions
     * @return number of affected rows
     */
    default int updateTableData(Connection connection, String catalog, String schema, String tableName,
                           Map<String, Object> values, Map<String, Object> whereConditions) {
        Logger log = LoggerFactory.getLogger(TableProvider.class);

        try {
            // Build SET clause
            StringJoiner setJoiner = new StringJoiner(", ");
            List<Object> setValues = new ArrayList<>();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                setJoiner.add("`" + entry.getKey() + "` = ?");
                setValues.add(entry.getValue());
            }

            // Build WHERE clause
            StringJoiner whereJoiner = new StringJoiner(" AND ");
            List<Object> whereValues = new ArrayList<>();
            for (Map.Entry<String, Object> entry : whereConditions.entrySet()) {
                whereJoiner.add("`" + entry.getKey() + "` = ?");
                whereValues.add(entry.getValue());
            }

            // Build SQL using constant
            String sql = String.format("UPDATE `%s` SET %s WHERE %s", tableName, setJoiner.toString(), whereJoiner.toString());

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int paramIndex = 1;
                for (Object value : setValues) {
                    ps.setObject(paramIndex++, value);
                }
                for (Object value : whereValues) {
                    ps.setObject(paramIndex++, value);
                }
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update data: " + e.getMessage(), e);
        }
    }

    /**
     * Delete data from table
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param tableName table name
     * @param whereConditions WHERE conditions
     * @return number of affected rows
     */
    default int deleteTableData(Connection connection, String catalog, String schema, String tableName,
                           Map<String, Object> whereConditions) {
        Logger log = LoggerFactory.getLogger(TableProvider.class);

        try {
            // Build WHERE clause
            StringJoiner whereJoiner = new StringJoiner(" AND ");
            List<Object> whereValues = new ArrayList<>();
            for (Map.Entry<String, Object> entry : whereConditions.entrySet()) {
                whereJoiner.add("`" + entry.getKey() + "` = ?");
                whereValues.add(entry.getValue());
            }

            // Build SQL using constant
            String sql = String.format("DELETE FROM `%s` WHERE %s", tableName, whereJoiner.toString());

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int paramIndex = 1;
                for (Object value : whereValues) {
                    ps.setObject(paramIndex++, value);
                }
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete data: " + e.getMessage(), e);
        }
    }
}
