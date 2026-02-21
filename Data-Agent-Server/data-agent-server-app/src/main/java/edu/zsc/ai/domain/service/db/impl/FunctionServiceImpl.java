package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.FunctionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final ConnectionService connectionService;
    private final DdlFetcher ddlFetcher;

    @Override
    public List<FunctionMetadata> listFunctions(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        return provider.getFunctions(active.connection(), catalog, schema);
    }

    @Override
    public String getFunctionDdl(Long connectionId, String catalog, String schema, String functionName, Long userId) {
        return ddlFetcher.fetch(connectionId, catalog, schema, functionName, userId, DdlResourceTypeEnum.FUNCTION,
                active -> {
                    FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
                    return provider.getFunctionDdl(active.connection(), catalog, schema, functionName);
                });
    }

    @Override
    public void deleteFunction(Long connectionId, String catalog, String schema, String functionName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        String dropSql = buildDropFunctionSql(schema, functionName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(active.connection());
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete function: " + result.getErrorMessage());
        }

        log.info("Function deleted successfully: connectionId={}, catalog={}, schema={}, functionName={}",
                connectionId, catalog, schema, functionName);
    }

    private String buildDropFunctionSql(String schema, String functionName) {
        StringBuilder sql = new StringBuilder("DROP FUNCTION ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(schema).append("`.");
        }
        sql.append("`").append(functionName).append("`");
        return sql.toString();
    }
}
