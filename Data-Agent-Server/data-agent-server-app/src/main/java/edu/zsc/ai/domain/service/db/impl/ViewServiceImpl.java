package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
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
    public List<String> getViews(DbContext db) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViews(active.connection(), db.catalog(), db.schema());
    }

    @Override
    public List<String> searchViews(DbContext db, String viewNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.searchViews(active.connection(), db.catalog(), db.schema(), viewNamePattern);
    }

    @Override
    public long countViews(DbContext db, String viewNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.countViews(active.connection(), db.catalog(), db.schema(), viewNamePattern);
    }

    @Override
    public long countViewRows(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViewDataCount(active.connection(), db.catalog(), db.schema(), viewName);
    }

    @Override
    public String getViewDdl(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        return provider.getViewDdl(active.connection(), db.catalog(), db.schema(), viewName);
    }

    @Override
    public void deleteView(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());
        provider.deleteView(active.connection(), db.catalog(), db.schema(), viewName);

        log.info("View deleted successfully: connectionId={}, catalog={}, schema={}, viewName={}",
                db.connectionId(), db.catalog(), db.schema(), viewName);
    }

    @Override
    public TableDataResponse getViewData(DbContext db, String viewName,
                                         Integer currentPage, Integer pageSize) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getViewDataCount(active.connection(), db.catalog(), db.schema(), viewName);

        SqlCommandResult result = provider.getViewData(active.connection(), db.catalog(), db.schema(), viewName, offset, pageSize);

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

    @Override
    public TableDataResponse getViewData(DbContext db, String viewName,
            Integer currentPage, Integer pageSize, String whereClause, String orderByColumn, String orderByDirection) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getViewDataCount(active.connection(), db.catalog(), db.schema(), viewName, whereClause);

        SqlCommandResult result = provider.getViewData(active.connection(), db.catalog(), db.schema(), viewName, offset, pageSize,
                whereClause, orderByColumn, orderByDirection);

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
