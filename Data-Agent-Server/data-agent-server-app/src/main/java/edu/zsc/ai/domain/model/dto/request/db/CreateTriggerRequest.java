package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateTriggerRequest extends BaseRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "databaseName is required")
    private String databaseName;

    private String schemaName;

    @NotBlank(message = "triggerName is required")
    private String triggerName;

    @NotBlank(message = "tableName is required")
    private String tableName;

    @NotBlank(message = "timing is required")
    private String timing; // BEFORE, AFTER

    @NotBlank(message = "event is required")
    private String event; // INSERT, UPDATE, DELETE

    @NotBlank(message = "body is required")
    private String body; // BEGIN...END
}
