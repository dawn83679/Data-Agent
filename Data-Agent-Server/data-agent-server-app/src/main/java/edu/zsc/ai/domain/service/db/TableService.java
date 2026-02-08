package edu.zsc.ai.domain.service.db;

import java.util.List;

public interface TableService {

    List<String> listTables(Long connectionId, String catalog, String schema);

    String getTableDdl(Long connectionId, String catalog, String schema, String tableName);
}
