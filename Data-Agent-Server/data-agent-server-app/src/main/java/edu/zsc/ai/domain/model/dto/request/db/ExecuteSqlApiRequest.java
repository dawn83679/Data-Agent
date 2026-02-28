package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HTTP request body for SQL execution API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlApiRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    private String databaseName;

    private String schemaName;

    @NotBlank(message = "sql is required")
    private String sql;
}

