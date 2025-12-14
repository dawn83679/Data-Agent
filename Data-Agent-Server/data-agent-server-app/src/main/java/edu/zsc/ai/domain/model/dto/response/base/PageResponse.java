package edu.zsc.ai.domain.model.dto.response.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic pagination response
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * Current page number
     */
    private Long current;

    /**
     * Page size
     */
    private Long size;

    /**
     * Total number of records
     */
    private Long total;

    /**
     * Total number of pages
     */
    private Long pages;

    /**
     * Data list for current page
     */
    private List<T> records;

    /**
     * Create page response from MyBatis-Plus page
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(page.getRecords())
                .build();
    }
}