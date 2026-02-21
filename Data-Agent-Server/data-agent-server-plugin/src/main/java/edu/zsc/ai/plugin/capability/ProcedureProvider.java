package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface ProcedureProvider {

    default List<ProcedureMetadata> getProcedures(Connection connection, String catalog, String schema) {
        try {
            List<ProcedureMetadata> list = new ArrayList<>();
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getProcedures(catalog, schema, null)) {
                while (rs.next()) {
                    short procType = rs.getShort(JdbcMetaDataConstants.PROCEDURE_TYPE);
                    if (procType == DatabaseMetaData.procedureResultUnknown
                            || procType == DatabaseMetaData.procedureNoResult
                            || procType == DatabaseMetaData.procedureReturnsResult) {
                        String name = rs.getString(JdbcMetaDataConstants.PROCEDURE_NAME);
                        if (StringUtils.isNotBlank(name)) {
                            list.add(new ProcedureMetadata(name));
                        }
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list procedures: " + e.getMessage(), e);
        }
    }

    default String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        throw new UnsupportedOperationException("Plugin does not support getting procedure DDL");
    }

    default void deleteProcedure(Connection connection, String pluginId, String catalog, String schema, String procedureName) {
        Logger log = LoggerFactory.getLogger(ProcedureProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = buildDropProcedureSql(schema, procedureName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete procedure: " + result.getErrorMessage());
        }

        log.info("Procedure deleted successfully: catalog={}, schema={}, procedureName={}", catalog, schema, procedureName);
    }

    default String buildDropProcedureSql(String schema, String procedureName) {
        StringBuilder sql = new StringBuilder("DROP PROCEDURE ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(schema).append("`.");
        }
        sql.append("`").append(procedureName).append("`");
        return sql.toString();
    }
}
