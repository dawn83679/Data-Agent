package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Table data response with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDataResponse {

    /**
     * Column headers
     */
    private List<String> headers;

    /**
     * Data rows, each row is a list of column values in the same order as columns
     */
    private List<List<Object>> rows;

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
