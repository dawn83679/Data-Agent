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
public class DeleteDatabaseRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    @NotBlank(message = "databaseName is required")
    private String databaseName;
}
