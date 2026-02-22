package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TableDataService;
import edu.zsc.ai.plugin.capability.TableProvider;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableDataServiceImpl implements TableDataService {

    private final ConnectionService connectionService;

    @Override
    public TableDataResponse getTableData(Long connectionId, String catalog, String schema, String tableName, Long userId, Integer currentPage, Integer pageSize) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        // Get total count
        long totalCount = provider.getTableDataCount(active.connection(), catalog, schema, tableName);

        // Get data
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        try (ResultSet rs = provider.getTableData(active.connection(), catalog, schema, tableName, offset, pageSize)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Get column names
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            // Get rows
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table data: " + e.getMessage(), e);
        }

        long totalPages = (totalCount + pageSize - 1) / pageSize;

        return TableDataResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalCount(totalCount)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public TableDataResponse getViewData(Long connectionId, String catalog, String schema, String viewName, Long userId, Integer currentPage, Integer pageSize) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int offset = (currentPage - 1) * pageSize;

        // Get total count
        long totalCount = provider.getViewDataCount(active.connection(), catalog, schema, viewName);

        // Get data
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        try (ResultSet rs = provider.getViewData(active.connection(), catalog, schema, viewName, offset, pageSize)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Get column names
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            // Get rows
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get view data: " + e.getMessage(), e);
        }

        long totalPages = (totalCount + pageSize - 1) / pageSize;

        return TableDataResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalCount(totalCount)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }
}
