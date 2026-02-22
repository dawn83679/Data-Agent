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

public interface ViewProvider {

    /**
     * Escapes backticks in an identifier for MySQL by doubling them
     */
    default String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.replace("`", "``");
    }

    default List<String> getViews(Connection connection, String catalog, String schema) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getTables(
                    catalog, schema, null, new String[] { JdbcMetaDataConstants.TABLE_TYPE_VIEW })) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_NAME);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list views: " + e.getMessage(), e);
        }
    }

    String getViewDdl(Connection connection, String catalog, String schema, String viewName);

    default void deleteView(Connection connection, String pluginId, String catalog, String schema, String viewName) {
        Logger log = LoggerFactory.getLogger(ViewProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = buildDropViewSql(schema, viewName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete view: " + result.getErrorMessage());
        }

        log.info("View deleted successfully: catalog={}, schema={}, viewName={}", catalog, schema, viewName);
    }

    default String buildDropViewSql(String schema, String viewName) {
        StringBuilder sql = new StringBuilder("DROP VIEW ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(escapeIdentifier(schema)).append("`.");
        }
        sql.append("`").append(escapeIdentifier(viewName)).append("`");
        return sql.toString();
    }

    /**
     * Get view data with pagination
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param viewName view name
     * @param offset offset for pagination
     * @param pageSize page size
     * @return view data as ResultSet (caller must close it)
     */
    default ResultSet getViewData(Connection connection, String catalog, String schema, String viewName, int offset, int pageSize) {
        throw new UnsupportedOperationException("Plugin does not support getting view data");
    }

    /**
     * Get total count of view rows
     * @param connection database connection
     * @param catalog catalog name (may be null)
     * @param schema schema name (may be null)
     * @param viewName view name
     * @return total row count
     */
    default long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        throw new UnsupportedOperationException("Plugin does not support getting view data count");
    }
}
