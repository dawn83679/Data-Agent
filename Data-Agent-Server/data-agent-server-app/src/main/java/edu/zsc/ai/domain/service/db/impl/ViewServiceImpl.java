package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceImpl implements ViewService {

    private final ConnectionService connectionService;

    @Override
    public List<String> getViews(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViews(active.connection(), catalog, schema);
    }

    @Override
    public String getViewDdl(Long connectionId, String catalog, String schema, String viewName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViewDdl(active.connection(), catalog, schema, viewName);
    }

    @Override
    public void deleteView(Long connectionId, String catalog, String schema, String viewName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        provider.deleteView(active.connection(), catalog, schema, viewName);

        log.info("View deleted successfully: connectionId={}, catalog={}, schema={}, viewName={}",
                connectionId, catalog, schema, viewName);
    }

    @Override
    public TableDataResponse getViewData(Long connectionId, String catalog, String schema, String viewName, Long userId, Integer currentPage, Integer pageSize) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getViewDataCount(active.connection(), catalog, schema, viewName);

        SqlCommandResult result = provider.getViewData(active.connection(), catalog, schema, viewName, offset, pageSize);

        long totalPages = (totalCount + pageSize - 1) / pageSize;

        return TableDataResponse.builder()
                .headers(result.getHeaders())
                .rows(result.getRows())
                .totalCount(totalCount)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
}
