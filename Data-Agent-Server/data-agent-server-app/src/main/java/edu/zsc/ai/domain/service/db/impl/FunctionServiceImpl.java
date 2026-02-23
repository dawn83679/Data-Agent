package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.capability.FunctionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
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

    @Override
    public List<FunctionMetadata> getFunctions(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        return provider.getFunctions(active.connection(), catalog, schema);
    }

    @Override
    public String getFunctionDdl(Long connectionId, String catalog, String schema, String functionName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        return provider.getFunctionDdl(active.connection(), catalog, schema, functionName);
    }

    @Override
    public void deleteFunction(Long connectionId, String catalog, String schema, String functionName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        provider.deleteFunction(active.connection(), catalog, schema, functionName);

        log.info("Function deleted successfully: connectionId={}, catalog={}, schema={}, functionName={}",
                connectionId, catalog, schema, functionName);
    }
}
