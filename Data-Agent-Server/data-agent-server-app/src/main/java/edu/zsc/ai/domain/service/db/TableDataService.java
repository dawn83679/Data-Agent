package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

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
}
