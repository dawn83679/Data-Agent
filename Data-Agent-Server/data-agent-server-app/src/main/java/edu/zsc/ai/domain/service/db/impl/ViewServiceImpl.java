package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.plugin.capability.ViewManager;
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

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getViews(borrowed.connection(), db.catalog(), db.schema());
        }
    }

    @Override
    public List<String> searchViews(DbContext db, String viewNamePattern) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.searchViews(borrowed.connection(), db.catalog(), db.schema(), viewNamePattern);
        }
    }

    @Override
    public long countViews(DbContext db, String viewNamePattern) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.countViews(borrowed.connection(), db.catalog(), db.schema(), viewNamePattern);
        }
    }

    @Override
    public long countViewRows(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getViewDataCount(borrowed.connection(), db.catalog(), db.schema(), viewName);
        }
    }

    @Override
    public String getViewDdl(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getViewDdl(borrowed.connection(), db.catalog(), db.schema(), viewName);
        }
    }

    @Override
    public void deleteView(DbContext db, String viewName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            provider.deleteView(borrowed.connection(), db.catalog(), db.schema(), viewName);
        }

        log.info("View deleted successfully: connectionId={}, catalog={}, schema={}, viewName={}",
                db.connectionId(), db.catalog(), db.schema(), viewName);
    }

    @Override
    public TableDataResponse getViewData(DbContext db, String viewName,
                                         Integer currentPage, Integer pageSize) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        int offset = (currentPage - 1) * pageSize;
        long totalCount;
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            totalCount = provider.getViewDataCount(borrowed.connection(), db.catalog(), db.schema(), viewName);
            result = provider.getViewData(borrowed.connection(), db.catalog(), db.schema(), viewName, offset, pageSize);
        }

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

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ViewManager provider = DefaultPluginManager.getInstance().getViewManagerByPluginId(active.pluginId());
        int offset = (currentPage - 1) * pageSize;
        long totalCount;
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            totalCount = provider.getViewDataCount(borrowed.connection(), db.catalog(), db.schema(), viewName, whereClause);
            result = provider.getViewData(borrowed.connection(), db.catalog(), db.schema(), viewName, offset, pageSize,
                    whereClause, orderByColumn, orderByDirection);
        }

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
