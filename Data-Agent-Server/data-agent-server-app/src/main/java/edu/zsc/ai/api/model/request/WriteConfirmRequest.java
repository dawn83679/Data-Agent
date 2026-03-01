package edu.zsc.ai.api.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for write-confirmation endpoints (/confirm and /cancel).
 */
@Data
@NoArgsConstructor
public class WriteConfirmRequest {

    @NotBlank(message = "confirmationToken cannot be empty")
    private String confirmationToken;
}
