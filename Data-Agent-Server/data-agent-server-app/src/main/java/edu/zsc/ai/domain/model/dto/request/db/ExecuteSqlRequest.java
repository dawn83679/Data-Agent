package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.api.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * SQL Execution Request for REST API
 * Used by SqlExecutionController for REST API calls
 * userId is injected by Controller from Sa-Token, not from request body
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExecuteSqlRequest extends BaseRequest {

    @NotBlank(message = "SQL cannot be null or empty")
    private String sql;
}
