package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.SqlExecutionConverter;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TableService;
import edu.zsc.ai.plugin.capability.TableManager;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.db.TableRowValue;
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

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getTableNames(borrowed.connection(), db.catalog(), db.schema());
        }
    }

    @Override
    public List<String> searchTables(DbContext db, String tableNamePattern) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.searchTables(borrowed.connection(), db.catalog(), db.schema(), tableNamePattern);
        }
    }

    @Override
    public long countTables(DbContext db, String tableNamePattern) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.countTables(borrowed.connection(), db.catalog(), db.schema(), tableNamePattern);
        }
    }

    @Override
    public String getTableDdl(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getTableDdl(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }
    }

    @Override
    public long countTableRows(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getTableDataCount(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }
    }

    @Override
    public void deleteTable(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            provider.deleteTable(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }

        log.info("Table deleted successfully: connectionId={}, catalog={}, schema={}, tableName={}",
                db.connectionId(), db.catalog(), db.schema(), tableName);
    }

    @Override
    public ExecuteSqlResponse insertRow(DbContext db, String tableName, List<TableRowValue> values) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            result = provider.insertRow(borrowed.connection(), db.catalog(), db.schema(), tableName, values);
        }
        return toExecuteSqlResponse(result, db);
    }

    @Override
    public ExecuteSqlResponse deleteRow(DbContext db, String tableName, List<TableRowValue> matchValues, boolean force) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            result = provider.deleteRow(borrowed.connection(), db.catalog(), db.schema(), tableName, matchValues, force);
        }
        return toExecuteSqlResponse(result, db);
    }

    @Override
    public TableDataResponse getTableData(DbContext db, String tableName,
                                          Integer currentPage, Integer pageSize) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        int offset = (currentPage - 1) * pageSize;
        long totalCount;
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            totalCount = provider.getTableDataCount(borrowed.connection(), db.catalog(), db.schema(), tableName);
            result = provider.getTableData(borrowed.connection(), db.catalog(), db.schema(), tableName, offset, pageSize);
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
    public TableDataResponse getTableData(DbContext db, String tableName,
            Integer currentPage, Integer pageSize, String whereClause, String orderByColumn, String orderByDirection) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        TableManager provider = DefaultPluginManager.getInstance().getTableManagerByPluginId(active.pluginId());
        int offset = (currentPage - 1) * pageSize;
        long totalCount;
        SqlCommandResult result;
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            totalCount = provider.getTableDataCount(borrowed.connection(), db.catalog(), db.schema(), tableName, whereClause);
            result = provider.getTableData(borrowed.connection(), db.catalog(), db.schema(), tableName, offset, pageSize,
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

    private ExecuteSqlResponse toExecuteSqlResponse(SqlCommandResult result, DbContext db) {
        ExecuteSqlResponse response = SqlExecutionConverter.toResponse(result);
        if (response != null) {
            response.setDatabaseName(db.catalog());
            response.setSchemaName(db.schema());
        }
        return response;
    }
}
