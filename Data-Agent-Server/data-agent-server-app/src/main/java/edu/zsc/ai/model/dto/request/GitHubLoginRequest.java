package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * GitHub OAuth Login Request
 *
 * @author Data-Agent Team
 */
@Data
public class GitHubLoginRequest {

    /**
     * Authorization code from GitHub OAuth callback
     */
    @NotBlank(message = "Authorization code is required")
    private String code;
}
