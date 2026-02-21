package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.ProcedureProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcedureServiceImpl implements ProcedureService {

    private final ConnectionService connectionService;
    private final DdlFetcher ddlFetcher;

    @Override
    public List<ProcedureMetadata> listProcedures(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.getProcedures(active.connection(), catalog, schema);
    }

    @Override
    public String getProcedureDdl(Long connectionId, String catalog, String schema, String procedureName, Long userId) {
        return ddlFetcher.fetch(connectionId, catalog, schema, procedureName, userId, DdlResourceTypeEnum.PROCEDURE,
                active -> {
                    ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
                    return provider.getProcedureDdl(active.connection(), catalog, schema, procedureName);
                });
    }

    @Override
    public void deleteProcedure(Long connectionId, String catalog, String schema, String procedureName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        String dropSql = buildDropProcedureSql(schema, procedureName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(active.connection());
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete procedure: " + result.getErrorMessage());
        }

        log.info("Procedure deleted successfully: connectionId={}, catalog={}, schema={}, procedureName={}",
                connectionId, catalog, schema, procedureName);
    }

    private String buildDropProcedureSql(String schema, String procedureName) {
        StringBuilder sql = new StringBuilder("DROP PROCEDURE ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(schema).append("`.");
        }
        sql.append("`").append(procedureName).append("`");
        return sql.toString();
    }
}
