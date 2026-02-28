package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlResultSet {
    private List<ExecuteSqlColumn> columns;
    private List<List<Object>> rows;

    /**
     * Actual fetched rows count, may differ from total.
     */
    private Integer fetchRows;

    /**
     * Whether result is truncated (client or server-side limit).
     */
    private Boolean truncated;
}
