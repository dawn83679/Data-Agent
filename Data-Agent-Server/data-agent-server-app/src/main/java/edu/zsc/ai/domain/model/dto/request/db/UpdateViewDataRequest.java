package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateViewDataRequest extends BaseRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "databaseName is required")
    private String databaseName;

    @NotBlank(message = "viewName is required")
    private String viewName;

    /**
     * Column names and values to update
     */
    @NotNull(message = "values is required")
    private Map<String, Object> values;

    /**
     * WHERE conditions (column -> value)
     */
    @NotNull(message = "whereConditions is required")
    private Map<String, Object> whereConditions;
}
