package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteProcedureRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "procedureName is required")
    private String procedureName;

    private String catalog;

    private String schema;
}
