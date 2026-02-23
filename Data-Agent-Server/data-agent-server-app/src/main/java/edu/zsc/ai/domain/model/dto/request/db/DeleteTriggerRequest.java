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
public class DeleteTriggerRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "triggerName is required")
    private String triggerName;

    private String catalog;

    private String schema;
}
