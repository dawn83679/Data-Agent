package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Table data response with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDataResponse {

    /**
     * Column names
     */
    private List<String> columns;

    /**
     * Data rows, each row is a list of column values
     */
    private List<Map<String, Object>> rows;

    /**
     * Total number of records
     */
    private Long totalCount;

    /**
     * Current page number
     */
    private Integer currentPage;

    /**
     * Page size
     */
    private Integer pageSize;

    /**
     * Total pages
     */
    private Long totalPages;
}
