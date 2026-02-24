package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * SQL Execution Request for AI Agent Tool
 * Used by ExecuteSqlTool for Agent/LLM calls
 * userId is required and obtained from InvocationParameters
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentExecuteSqlRequest extends BaseRequest {

    @NotBlank(message = "SQL cannot be null or empty")
    private String sql;

    @NotNull(message = "User id cannot be null")
    private Long userId;
}
