package edu.zsc.ai.domain.service.oauth.impl;

import edu.zsc.ai.common.constant.OAuthConstant;
import edu.zsc.ai.config.sys.OAuthProperties;
import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;
import edu.zsc.ai.domain.model.enums.AuthProviderEnum;
import edu.zsc.ai.domain.service.oauth.OAuthStrategy;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubOAuthStrategy implements OAuthStrategy {

    private final OAuthProperties oauthProperties;
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return AuthProviderEnum.GITHUB.getValue();
    }

    @Override
    public String getAuthorizationUrl(String state) {
        OAuthProperties.Registration registration = oauthProperties.getClients()
                .get(OAuthConstant.GITHUB_PROVIDER);
        BusinessException.assertNotNull(registration, "GitHub OAuth configuration not found");

        return UriComponentsBuilder.fromUriString(OAuthConstant.GITHUB_AUTHORIZATION_URI)
                .queryParam(OAuthConstant.PARAM_CLIENT_ID, registration.getClientId())
                .queryParam(OAuthConstant.PARAM_REDIRECT_URI, registration.getRedirectUri())
                .queryParam(OAuthConstant.PARAM_SCOPE, OAuthConstant.GITHUB_SCOPE) // Request email permission
                .queryParam(OAuthConstant.PARAM_STATE, state)
                .queryParam(OAuthConstant.PARAM_PROMPT, OAuthConstant.PARAM_PROMPT_VALUE_SELECT_ACCOUNT)
                .build().toUriString();
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        OAuthProperties.Registration registration = oauthProperties.getClients()
                .get(OAuthConstant.GITHUB_PROVIDER);
        BusinessException.assertNotNull(registration, "GitHub OAuth configuration not found");

        // 1. Exchange code for access token
        String accessToken = getAccessToken(code, registration);

        // 2. Get user profile
        Map<String, Object> userProfile = getUserProfile(accessToken);

        // 3. Get primary email (if not in profile)
        String email = (String) userProfile.get(OAuthConstant.KEY_EMAIL);
        if (email == null || email.isEmpty()) {
            email = getPrimaryEmail(accessToken);
        }

        String id = String.valueOf(userProfile.get(OAuthConstant.KEY_ID));
        String name = (String) userProfile.get(OAuthConstant.KEY_NAME);
        if (name == null) {
            name = (String) userProfile.get(OAuthConstant.KEY_LOGIN);
        }
        String avatarUrl = (String) userProfile.get(OAuthConstant.KEY_AVATAR_URL);

        return OAuthUserInfo.builder()
                .provider(AuthProviderEnum.GITHUB.getValue())
                .providerId(id)
                .email(email)
                .nickname(name)
                .avatarUrl(avatarUrl)
                .build();
    }

    private String getAccessToken(String code, OAuthProperties.Registration registration) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuthConstant.PARAM_CLIENT_ID, registration.getClientId());
        map.add(OAuthConstant.PARAM_CLIENT_SECRET, registration.getClientSecret());
        map.add(OAuthConstant.PARAM_CODE, code);
        map.add(OAuthConstant.PARAM_REDIRECT_URI, registration.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OAuthConstant.GITHUB_TOKEN_URI,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });
            Map<String, Object> body = response.getBody();
            BusinessException.throwIf(body == null || !body.containsKey(OAuthConstant.KEY_ACCESS_TOKEN),
                    "Failed to retrieve access token from GitHub");
            return (String) body.get(OAuthConstant.KEY_ACCESS_TOKEN);
        } catch (Exception e) {
            log.error("GitHub token exchange error", e);
            throw new BusinessException("GitHub login failed: " + e.getMessage());
        }
    }

    private Map<String, Object> getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OAuthConstant.GITHUB_USER_INFO_URI,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            log.error("GitHub user info error", e);
            throw new BusinessException("Failed to get GitHub user info");
        }
    }

    private String getPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    OAuthConstant.GITHUB_USER_EMAILS_URI,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                for (Map<String, Object> emailObj : emails) {
                    Boolean primary = (Boolean) emailObj.get(OAuthConstant.KEY_PRIMARY);
                    Boolean verified = (Boolean) emailObj.get(OAuthConstant.KEY_VERIFIED);
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailObj.get(OAuthConstant.KEY_EMAIL);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get GitHub emails", e);
            return null;
        }
    }
}
