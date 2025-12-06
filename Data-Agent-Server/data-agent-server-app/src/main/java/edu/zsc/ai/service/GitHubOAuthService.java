package edu.zsc.ai.service;

import edu.zsc.ai.model.dto.response.GitHubTokenResponse;
import edu.zsc.ai.model.dto.response.GitHubUserInfo;

/**
 * GitHub OAuth Service
 * Handles GitHub OAuth 2.0 authentication flow
 *
 * @author Data-Agent Team
 */
public interface GitHubOAuthService {

    /**
     * Generate GitHub OAuth authorization URL
     *
     * @param state CSRF protection state parameter
     * @return Authorization URL to redirect user to
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
     * @param accessToken Access token from GitHub
     * @return User information
     */
    GitHubUserInfo getUserInfo(String accessToken);

    /**
     * Store OAuth state in Redis for CSRF protection
     *
     * @param state generated state value
     */
    void storeState(String state);

    /**
     * Validate OAuth state from callback
     *
     * @param state state value from callback
     * @return true if valid, false otherwise
     */
    boolean validateState(String state);

    /**
     * Generate secure random state parameter for CSRF protection
     *
     * @return generated state string
     */
    String generateState();
}
