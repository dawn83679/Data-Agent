package edu.zsc.ai.domain.service.oauth.impl;

import edu.zsc.ai.common.constant.OAuthConstant;
import edu.zsc.ai.config.sys.OAuthProperties;
import edu.zsc.ai.domain.model.dto.oauth.OAuthUserInfo;
import edu.zsc.ai.domain.model.enums.AuthProviderEnum;
import edu.zsc.ai.domain.service.oauth.OAuthStrategy;
import edu.zsc.ai.util.JwtUtil;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthStrategy implements OAuthStrategy {

    private final OAuthProperties oauthProperties;
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return AuthProviderEnum.GOOGLE.name();
    }

    @Override
    public String getAuthorizationUrl(String state) {
        OAuthProperties.Registration registration = oauthProperties.getClients()
                .get(OAuthConstant.GOOGLE_PROVIDER);
        BusinessException.assertNotNull(registration, "Google OAuth configuration not found");

        return UriComponentsBuilder.fromUriString(OAuthConstant.GOOGLE_AUTHORIZATION_URI)
                .queryParam(OAuthConstant.PARAM_CLIENT_ID, registration.getClientId())
                .queryParam(OAuthConstant.PARAM_REDIRECT_URI, registration.getRedirectUri())
                .queryParam(OAuthConstant.PARAM_RESPONSE_TYPE, OAuthConstant.GOOGLE_RESPONSE_TYPE)
                .queryParam(OAuthConstant.PARAM_SCOPE, OAuthConstant.GOOGLE_SCOPE)
                .queryParam(OAuthConstant.PARAM_STATE, state)
                .queryParam(OAuthConstant.PARAM_ACCESS_TYPE, OAuthConstant.GOOGLE_ACCESS_TYPE)
                .build().toUriString();
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        OAuthProperties.Registration registration = oauthProperties.getClients()
                .get(OAuthConstant.GOOGLE_PROVIDER);
        BusinessException.assertNotNull(registration, "Google OAuth configuration not found");

        // Exchange code for token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OAuthConstant.PARAM_CLIENT_ID, registration.getClientId());
        map.add(OAuthConstant.PARAM_CLIENT_SECRET, registration.getClientSecret());
        map.add(OAuthConstant.PARAM_CODE, code);
        map.add(OAuthConstant.PARAM_GRANT_TYPE, OAuthConstant.GOOGLE_GRANT_TYPE);
        map.add(OAuthConstant.PARAM_REDIRECT_URI, registration.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OAuthConstant.GOOGLE_TOKEN_URI,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });
            Map<String, Object> body = response.getBody();

            BusinessException.throwIf(body == null || !body.containsKey(OAuthConstant.KEY_ID_TOKEN),
                    "Failed to retrieve ID token from Google");

            String idToken = (String) body.get(OAuthConstant.KEY_ID_TOKEN);

            // Parse ID Token (JWT)
            // Note: In production, signature verification is recommended.
            // Here we trust the direct response from Google's HTTPS endpoint.
            String email = JwtUtil.getClaimAsString(idToken, OAuthConstant.KEY_EMAIL);
            String name = JwtUtil.getClaimAsString(idToken, OAuthConstant.KEY_NAME);
            String picture = JwtUtil.getClaimAsString(idToken, OAuthConstant.KEY_PICTURE);
            String sub = JwtUtil.getClaimAsString(idToken, OAuthConstant.KEY_SUB); // Google's unique user ID

            return OAuthUserInfo.builder()
                    .provider(AuthProviderEnum.GOOGLE.name())
                    .providerId(sub)
                    .email(email)
                    .nickname(name)
                    .avatarUrl(picture)
                    .build();

        } catch (Exception e) {
            log.error("Google OAuth error", e);
            throw new BusinessException("Google login failed: " + e.getMessage());
        }
    }
}
