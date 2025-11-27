package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token Request
 *
 * @author Data-Agent Team
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
}
