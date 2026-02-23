package edu.zsc.ai.domain.service.db.impl;

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
    public List<String> getTables(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableNames(active.connection(), catalog, schema);
    }

    @Override
    public String getTableDdl(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        return provider.getTableDdl(active.connection(), catalog, schema, tableName);
    }

    @Override
    public void deleteTable(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());
        provider.deleteTable(active.connection(), catalog, schema, tableName);

        log.info("Table deleted successfully: connectionId={}, catalog={}, schema={}, tableName={}",
                connectionId, catalog, schema, tableName);
    }

    @Override
    public TableDataResponse getTableData(Long connectionId, String catalog, String schema, String tableName, Long userId, Integer currentPage, Integer pageSize) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        long totalCount = provider.getTableDataCount(active.connection(), catalog, schema, tableName);

        SqlCommandResult result = provider.getTableData(active.connection(), catalog, schema, tableName, offset, pageSize);

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
