package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface TableProvider {

    default List<String> getTableNames(Connection connection, String catalog, String schema) {
        return searchTables(connection, catalog, schema, null);
    }

    default List<String> searchTables(Connection connection, String catalog, String schema, String tableNamePattern) {
        try {
            List<String> list = new ArrayList<>();
            String pattern = StringUtils.isBlank(tableNamePattern) ? null : tableNamePattern;
            try (ResultSet rs = connection.getMetaData().getTables(
                    catalog, schema, pattern, new String[] {JdbcMetaDataConstants.TABLE_TYPE_TABLE})) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_NAME);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search tables: " + e.getMessage(), e);
        }
    }

    default String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support getting table DDL");
    }

    default void deleteTable(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support deleting table");
    }

    default SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName, int offset, int pageSize) {
        throw new UnsupportedOperationException("Plugin does not support getting table data");
    }

    default long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        throw new UnsupportedOperationException("Plugin does not support getting table data count");
    }

    /**
     * Get table data with optional WHERE clause and single-column ORDER BY.
     * @param whereClause optional WHERE condition (without "WHERE"), e.g. "status = 1"
     * @param orderByColumn optional column name for ordering
     * @param orderByDirection "asc" or "desc"
     */
    default SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName,
            int offset, int pageSize, String whereClause, String orderByColumn, String orderByDirection) {
        if (StringUtils.isBlank(whereClause) && StringUtils.isBlank(orderByColumn)) {
            return getTableData(connection, catalog, schema, tableName, offset, pageSize);
        }
        throw new UnsupportedOperationException("Plugin does not support filtered table data");
    }

    /**
     * Get table row count with optional WHERE clause.
     */
    default long getTableDataCount(Connection connection, String catalog, String schema, String tableName, String whereClause) {
        if (StringUtils.isBlank(whereClause)) {
            return getTableDataCount(connection, catalog, schema, tableName);
        }
        throw new UnsupportedOperationException("Plugin does not support filtered table count");
    }
}
