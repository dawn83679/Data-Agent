package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.dto.response.db.DataModificationResponse;
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

    @Override
    public DataModificationResponse insertData(Long connectionId, String databaseName, String schemaName, String tableName,
                                                List<String> columns, List<Map<String, Object>> valuesList, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int affectedRows = provider.insertTableData(active.connection(), databaseName, schemaName, tableName, columns, valuesList);

        log.info("Data inserted successfully: tableName={}, affectedRows={}", tableName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }

    @Override
    public DataModificationResponse updateData(Long connectionId, String databaseName, String schemaName, String tableName,
                                               Map<String, Object> values, Map<String, Object> whereConditions, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int affectedRows = provider.updateTableData(active.connection(), databaseName, schemaName, tableName, values, whereConditions);

        log.info("Data updated successfully: tableName={}, affectedRows={}", tableName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }

    @Override
    public DataModificationResponse deleteData(Long connectionId, String databaseName, String schemaName, String tableName,
                                               Map<String, Object> whereConditions, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        TableProvider provider = DefaultPluginManager.getInstance().getTableProviderByPluginId(active.pluginId());

        int affectedRows = provider.deleteTableData(active.connection(), databaseName, schemaName, tableName, whereConditions);

        log.info("Data deleted successfully: tableName={}, affectedRows={}", tableName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }

    @Override
    public DataModificationResponse insertViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                                  List<String> columns, List<Map<String, Object>> valuesList, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int affectedRows = provider.insertViewData(active.connection(), databaseName, schemaName, viewName, columns, valuesList);

        log.info("Data inserted into view successfully: viewName={}, affectedRows={}", viewName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }

    @Override
    public DataModificationResponse updateViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                                  Map<String, Object> values, Map<String, Object> whereConditions, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int affectedRows = provider.updateViewData(active.connection(), databaseName, schemaName, viewName, values, whereConditions);

        log.info("Data updated in view successfully: viewName={}, affectedRows={}", viewName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }

    @Override
    public DataModificationResponse deleteViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                                  Map<String, Object> whereConditions, Long userId) {
        connectionService.openConnection(connectionId, databaseName, schemaName, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, databaseName, schemaName, userId);

        ViewProvider provider = DefaultPluginManager.getInstance().getViewProviderByPluginId(active.pluginId());

        int affectedRows = provider.deleteViewData(active.connection(), databaseName, schemaName, viewName, whereConditions);

        log.info("Data deleted from view successfully: viewName={}, affectedRows={}", viewName, affectedRows);

        return DataModificationResponse.builder()
                .affectedRows(affectedRows)
                .build();
    }
}
