package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TableService;
import edu.zsc.ai.plugin.capability.TableProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
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
    public List<String> getTables(DbContext db) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableNames(active.connection(), db.catalog(), db.schema());
    }

    @Override
    public List<String> searchTables(DbContext db, String tableNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.searchTables(active.connection(), db.catalog(), db.schema(), tableNamePattern);
    }

    @Override
    public long countTables(DbContext db, String tableNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.countTables(active.connection(), db.catalog(), db.schema(), tableNamePattern);
    }

    @Override
    public String getTableDdl(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableDdl(active.connection(), db.catalog(), db.schema(), tableName);
    }

    @Override
    public long countTableRows(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableDataCount(active.connection(), db.catalog(), db.schema(), tableName);
    }

    @Override
    public void deleteTable(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        provider.deleteTable(active.connection(), db.catalog(), db.schema(), tableName);

        log.info("Table deleted successfully: connectionId={}, catalog={}, schema={}, tableName={}",
                db.connectionId(), db.catalog(), db.schema(), tableName);
    }

    @Override
    public TableDataResponse getTableData(DbContext db, String tableName,
                                          Integer currentPage, Integer pageSize) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getTableDataCount(active.connection(), db.catalog(), db.schema(), tableName);

        SqlCommandResult result = provider.getTableData(active.connection(), db.catalog(), db.schema(), tableName, offset, pageSize);

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
    public TableDataResponse getTableData(DbContext db, String tableName,
            Integer currentPage, Integer pageSize, String whereClause, String orderByColumn, String orderByDirection) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getTableDataCount(active.connection(), db.catalog(), db.schema(), tableName, whereClause);

        SqlCommandResult result = provider.getTableData(active.connection(), db.catalog(), db.schema(), tableName, offset, pageSize,
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
