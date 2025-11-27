package edu.zsc.ai.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Pair Response
 * Contains Access Token and Refresh Token
 *
 * @author Data-Agent Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPairResponse {

    /**
     * Access Token (JWT, short-lived)
     */
    private String accessToken;

    /**
     * Refresh Token (random string, long-lived)
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer")
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration time (seconds)
     */
    private Long expiresIn;
}
