package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.api.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * SQL Execution Request for AI Agent Tool
 * Used by SelectSqlTool/WriteSqlTool for Agent/LLM calls.
 * userId is obtained from RequestContext (set by ToolContext or SaTokenConfigure interceptor).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentExecuteSqlRequest extends BaseRequest {

    @NotBlank(message = "SQL cannot be null or empty")
    private String sql;
}
