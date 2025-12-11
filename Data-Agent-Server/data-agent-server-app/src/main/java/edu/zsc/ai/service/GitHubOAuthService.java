package edu.zsc.ai.service;

import edu.zsc.ai.model.dto.response.GitHubTokenResponse;
import edu.zsc.ai.model.dto.response.GitHubUserInfo;

/**
 * GitHub OAuth Service Interface
 *
 * @author Data-Agent Team
 */
public interface GitHubOAuthService {

    /**
     * Get GitHub OAuth authorization URL
     *
     * @param state CSRF protection state parameter
     * @return Authorization URL
     */
    String getAuthorizationUrl(String state);

    /**
     * Exchange authorization code for access token
     *
     * @param code Authorization code from GitHub
     * @return Token response containing access_token
     */
    GitHubTokenResponse exchangeCode(String code);

    /**
     * Get user information using access token
     *
     * @param accessToken GitHub access token
     * @return User information
     */
    GitHubUserInfo getUserInfo(String accessToken);

    /**
     * Generate a secure random state parameter for CSRF protection
     *
     * @return Random state string
     */
    String generateState();

    /**
     * Store state parameter for validation
     *
     * @param state State parameter to store
     */
    void storeState(String state);

    /**
     * Validate state parameter
     *
     * @param state State parameter to validate
     * @return true if valid, false otherwise
     */
    boolean validateState(String state);
}
