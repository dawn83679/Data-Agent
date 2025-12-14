package edu.zsc.ai.domain.service.oauth;

import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;

public interface OAuthStrategy {
    /**
     * Get provider name (used for strategy routing)
     * 
     * @return provider name (e.g., "google", "github")
     */
    String getProviderName();

    /**
     * Generate authorization redirect URL
     * 
     * @param state Pass-through parameter (usually fromUrl)
     * @return Authorization URL
     */
    String getAuthorizationUrl(String state);

    /**
     * Process callback and get user info
     * 
     * @param code Authorization code
     * @return Standardized user info
     */
    OAuthUserInfo getUserInfo(String code);
}
