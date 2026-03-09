package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

import java.util.List;

public interface TableService {

    List<String> getTables(DbContext db);

    List<String> searchTables(DbContext db, String tableNamePattern);

    long countTables(DbContext db, String tableNamePattern);

    long countTableRows(DbContext db, String tableName);

    String getTableDdl(DbContext db, String tableName);

    void deleteTable(DbContext db, String tableName);

    TableDataResponse getTableData(DbContext db, String tableName, Integer currentPage, Integer pageSize);

    TableDataResponse getTableData(DbContext db, String tableName,
            Integer currentPage, Integer pageSize, String whereClause, String orderByColumn, String orderByDirection);
}
