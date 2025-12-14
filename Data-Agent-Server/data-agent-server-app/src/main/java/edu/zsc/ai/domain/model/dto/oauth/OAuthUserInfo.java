package edu.zsc.ai.domain.model.dto.oauth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthUserInfo {
    private String provider; // "google", "github"
    private String providerId; // Third-party platform unique ID (sub/id)
    private String email; // Email
    private String nickname; // Nickname
    private String avatarUrl; // Avatar URL
}
