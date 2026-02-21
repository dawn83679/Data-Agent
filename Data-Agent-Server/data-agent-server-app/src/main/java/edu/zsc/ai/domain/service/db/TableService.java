package edu.zsc.ai.domain.service.db;

import java.util.List;

public interface TableService {

    List<String> listTables(Long connectionId, String catalog, String schema, Long userId);

    String getTableDdl(Long connectionId, String catalog, String schema, String tableName, Long userId);

    void deleteTable(Long connectionId, String catalog, String schema, String tableName, Long userId);
}
