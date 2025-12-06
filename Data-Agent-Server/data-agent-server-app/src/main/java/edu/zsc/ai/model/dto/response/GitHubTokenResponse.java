package edu.zsc.ai.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * GitHub OAuth Token Response
 *
 * @author Data-Agent Team
 */
@Data
public class GitHubTokenResponse {

    /**
     * Access token for API requests
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Token type (usually "bearer")
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Scope of access granted
     */
    @JsonProperty("scope")
    private String scope;
}
