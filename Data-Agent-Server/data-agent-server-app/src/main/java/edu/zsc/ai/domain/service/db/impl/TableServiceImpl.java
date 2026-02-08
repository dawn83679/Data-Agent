package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TableService;
import edu.zsc.ai.plugin.capability.TableProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {

    private final ConnectionService connectionService;

    @Override
    public List<String> listTables(Long connectionId, String catalog, String schema) {
        connectionService.openConnection(connectionId, catalog, schema);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableNames(active.connection(), catalog, schema);
    }

    @Override
    public String getTableDdl(Long connectionId, String catalog, String schema, String tableName) {
        log.info("Getting DDL for table: connectionId={}, catalog={}, schema={}, tableName={}", 
                connectionId, catalog, schema, tableName);
        
        connectionService.openConnection(connectionId, catalog, schema);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        
        String ddl = provider.getTableDdl(active.connection(), catalog, schema, tableName);
        
        log.debug("Successfully retrieved DDL for table: {}", tableName);
        return ddl;
    }
}
