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
public class DeleteViewRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "viewName is required")
    private String viewName;

    private String catalog;

    private String schema;
}
