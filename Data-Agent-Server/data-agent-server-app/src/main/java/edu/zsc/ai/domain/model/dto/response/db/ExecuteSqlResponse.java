package edu.zsc.ai.domain.model.dto.response.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of executing a single SQL statement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlResponse {

    private boolean success;

    private String errorMessage;

    private long executionTimeMs;

    private String originalSql;

    private String executedSql;

    private String databaseName;

    private String schemaName;

    private boolean query;

    private List<String> headers;

    private List<List<Object>> rows;

    private int affectedRows;

    /**
     * Structured response aligned with DataGrip-style output.
     * Keep legacy fields above for backward compatibility.
     */
    private ExecuteSqlResponseType type;

    private ExecuteSqlResultSet resultSet;

    private ExecuteSqlExecutionInfo executionInfo;

    private List<ExecuteSqlMessage> messages;

    private List<ExecuteSqlSubResult> results;
}
