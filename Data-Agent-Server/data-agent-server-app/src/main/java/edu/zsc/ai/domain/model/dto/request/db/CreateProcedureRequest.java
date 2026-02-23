package edu.zsc.ai.domain.model.dto.request.db;

import edu.zsc.ai.model.request.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateProcedureRequest extends BaseRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "databaseName is required")
    private String databaseName;

    private String schemaName;

    @NotBlank(message = "procedureName is required")
    private String procedureName;

    private List<ParameterDto> parameters;

    @NotBlank(message = "body is required")
    private String body;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDto {
        private String name;
        private String type;
        private String mode; // IN, OUT, INOUT
    }
}
