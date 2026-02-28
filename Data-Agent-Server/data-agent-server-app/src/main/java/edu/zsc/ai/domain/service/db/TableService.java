package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

import java.util.List;

public interface TableService {

    List<String> getTables(Long connectionId, String catalog, String schema, Long userId);

    List<String> searchTables(Long connectionId, String catalog, String schema, String tableNamePattern, Long userId);

    long countTableRows(Long connectionId, String catalog, String schema, String tableName, Long userId);

    String getTableDdl(Long connectionId, String catalog, String schema, String tableName, Long userId);

    void deleteTable(Long connectionId, String catalog, String schema, String tableName, Long userId);

    TableDataResponse getTableData(Long connectionId, String catalog, String schema, String tableName, Long userId, Integer currentPage, Integer pageSize);
}
