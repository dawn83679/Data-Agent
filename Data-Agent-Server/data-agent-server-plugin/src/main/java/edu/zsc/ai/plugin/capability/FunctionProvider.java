package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface FunctionProvider {

    default List<FunctionMetadata> getFunctions(Connection connection, String catalog, String schema) {
        try {
            List<FunctionMetadata> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getFunctions(catalog, schema, null)) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.FUNCTION_NAME);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(new FunctionMetadata(name));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list functions: " + e.getMessage(), e);
        }
    }

    default String getFunctionDdl(Connection connection, String catalog, String schema, String functionName) {
        throw new UnsupportedOperationException("Plugin does not support getting function DDL");
    }

    default void deleteFunction(Connection connection, String catalog, String schema, String functionName) {
        throw new UnsupportedOperationException("Plugin does not support deleting function");
    }
}
