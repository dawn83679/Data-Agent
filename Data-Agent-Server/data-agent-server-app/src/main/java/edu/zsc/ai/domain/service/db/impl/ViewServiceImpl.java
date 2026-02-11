package edu.zsc.ai.domain.service.db.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceImpl implements ViewService {

    private final ConnectionService connectionService;

    @Override
    public List<String> listViews(Long connectionId, String catalog, String schema) {
        connectionService.openConnection(connectionId, catalog, schema);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViews(active.connection(), catalog, schema);
    }

    @Override
    public String getViewDdl(Long connectionId, String catalog, String schema, String viewName) {
        log.info("Getting DDL for view: connectionId={}, catalog={}, schema={}, viewName={}", 
                connectionId, catalog, schema, viewName);
        
        connectionService.openConnection(connectionId, catalog, schema);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        
        String ddl = provider.getViewDdl(active.connection(), catalog, schema, viewName);
        
        log.debug("Successfully retrieved DDL for view: {}", viewName);
        return ddl;
    }
}
