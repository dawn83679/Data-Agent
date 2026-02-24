package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.response.db.DataModificationResponse;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

import java.util.List;
import java.util.Map;

public interface TableDataService {

    /**
     * Get table data with pagination
     *
     * @param connectionId connection ID
     * @param catalog catalog name
     * @param schema schema name
     * @param tableName table or view name
     * @param userId user ID
     * @param currentPage current page number
     * @param pageSize page size
     * @return table data response
     */
    TableDataResponse getTableData(Long connectionId, String catalog, String schema, String tableName, Long userId, Integer currentPage, Integer pageSize);

    /**
     * Get view data with pagination
     *
     * @param connectionId connection ID
     * @param catalog catalog name
     * @param schema schema name
     * @param viewName view name
     * @param userId user ID
     * @param currentPage current page number
     * @param pageSize page size
     * @return table data response
     */
    TableDataResponse getViewData(Long connectionId, String catalog, String schema, String viewName, Long userId, Integer currentPage, Integer pageSize);

    /**
     * Insert data into table
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param tableName table name
     * @param columns column names
     * @param valuesList list of value maps (each map represents a row)
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse insertData(Long connectionId, String databaseName, String schemaName, String tableName,
                                         List<String> columns, List<Map<String, Object>> valuesList, Long userId);

    /**
     * Update data in table
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param tableName table name
     * @param values column values to update
     * @param whereConditions WHERE conditions
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse updateData(Long connectionId, String databaseName, String schemaName, String tableName,
                                        Map<String, Object> values, Map<String, Object> whereConditions, Long userId);

    /**
     * Delete data from table
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param tableName table name
     * @param whereConditions WHERE conditions
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse deleteData(Long connectionId, String databaseName, String schemaName, String tableName,
                                         Map<String, Object> whereConditions, Long userId);

    /**
     * Insert data into view
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param viewName view name
     * @param columns column names
     * @param valuesList list of value maps (each map represents a row)
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse insertViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                          List<String> columns, List<Map<String, Object>> valuesList, Long userId);

    /**
     * Update data in view
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param viewName view name
     * @param values column values to update
     * @param whereConditions WHERE conditions
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse updateViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                           Map<String, Object> values, Map<String, Object> whereConditions, Long userId);

    /**
     * Delete data from view
     *
     * @param connectionId connection ID
     * @param databaseName database name
     * @param schemaName schema name
     * @param viewName view name
     * @param whereConditions WHERE conditions
     * @param userId user ID
     * @return data modification response
     */
    DataModificationResponse deleteViewData(Long connectionId, String databaseName, String schemaName, String viewName,
                                            Map<String, Object> whereConditions, Long userId);
}
