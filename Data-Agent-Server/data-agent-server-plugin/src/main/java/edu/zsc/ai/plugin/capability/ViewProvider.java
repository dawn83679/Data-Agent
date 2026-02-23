package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface ViewProvider {

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

    default String getViewDdl(Connection connection, String catalog, String schema, String viewName) {
        throw new UnsupportedOperationException("Plugin does not support getting view DDL");
    }

    default void deleteView(Connection connection, String catalog, String schema, String viewName) {
        throw new UnsupportedOperationException("Plugin does not support deleting view");
    }

    default SqlCommandResult getViewData(Connection connection, String catalog, String schema, String viewName, int offset, int pageSize) {
        throw new UnsupportedOperationException("Plugin does not support getting view data");
    }

    default long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        throw new UnsupportedOperationException("Plugin does not support getting view data count");
    }
}
