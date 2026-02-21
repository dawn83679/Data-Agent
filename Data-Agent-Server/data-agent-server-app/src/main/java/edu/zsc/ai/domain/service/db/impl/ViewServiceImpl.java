package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceImpl implements ViewService {

    private final ConnectionService connectionService;
    private final DdlFetcher ddlFetcher;

    @Override
    public List<String> listViews(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViews(active.connection(), catalog, schema);
    }

    @Override
    public String getViewDdl(Long connectionId, String catalog, String schema, String viewName, Long userId) {
        return ddlFetcher.fetch(connectionId, catalog, schema, viewName, userId, DdlResourceTypeEnum.VIEW,
                active -> {
                    ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
                    return provider.getViewDdl(active.connection(), catalog, schema, viewName);
                });
    }

    @Override
    public void deleteView(Long connectionId, String catalog, String schema, String viewName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        provider.deleteView(active.connection(), active.pluginId(), catalog, schema, viewName);

        log.info("View deleted successfully: connectionId={}, catalog={}, schema={}, viewName={}",
                connectionId, catalog, schema, viewName);
    }
}
