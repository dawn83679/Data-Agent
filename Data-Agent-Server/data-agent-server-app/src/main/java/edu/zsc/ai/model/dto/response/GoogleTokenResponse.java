package edu.zsc.ai.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Google OAuth Token Response
 * Response from Google's token endpoint after exchanging authorization code
 *
 * @author Data-Agent Team
 */
@Data
public class GoogleTokenResponse {

    /**
     * Access token for accessing Google APIs
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * ID token containing user identity information (JWT)
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * Refresh token for obtaining new access tokens
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Token expiration time in seconds
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * Token type (usually "Bearer")
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Granted scopes
     */
    @JsonProperty("scope")
    private String scope;
}
