package edu.zsc.ai.service;

import edu.zsc.ai.model.dto.response.GoogleTokenResponse;
import edu.zsc.ai.model.dto.response.GoogleUserInfo;

/**
 * Google OAuth Service
 * Handles Google OAuth 2.0 authentication flow
 *
 * @author Data-Agent Team
 */
public interface GoogleOAuthService {

    /**
     * Generate Google OAuth authorization URL
     *
     * @param state CSRF protection state parameter
     * @return Authorization URL to redirect user to
     */
    String getAuthorizationUrl(String state);

    /**
     * Exchange authorization code for tokens
     *
     * @param code Authorization code from Google
     * @return Token response containing access_token and id_token
     */
    GoogleTokenResponse exchangeCode(String code);

    /**
     * Validate ID token and extract user information
     *
     * @param idToken ID token from Google
     * @return User information extracted from token
     */
    GoogleUserInfo validateAndExtractUserInfo(String idToken);

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
}
