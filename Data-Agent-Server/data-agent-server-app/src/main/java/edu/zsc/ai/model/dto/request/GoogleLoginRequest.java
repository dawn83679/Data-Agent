package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Google OAuth Login Request
 *
 * @author Data-Agent Team
 */
@Data
public class GoogleLoginRequest {

    /**
     * Google OAuth authorization code
     */
    @NotBlank(message = "Authorization code cannot be empty")
    private String code;

    /**
     * Redirect URI (must match the one registered with Google)
     */
    private String redirectUri;
}
