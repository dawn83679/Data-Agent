package edu.zsc.ai.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.zsc.ai.config.properties.GoogleOAuthProperties;
import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.GoogleTokenResponse;
import edu.zsc.ai.model.dto.response.GoogleUserInfo;
import edu.zsc.ai.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_SCOPE = "openid email profile";

    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final long STATE_EXPIRATION_MINUTES = 10;

    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public String getAuthorizationUrl(String state) {
        if (!googleOAuthProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Google OAuth is not configured. Please set client ID and secret.");
        }

        // Build authorization URL with required parameters
        StringBuilder url = new StringBuilder(GOOGLE_AUTH_URL);
        url.append("?client_id=").append(urlEncode(googleOAuthProperties.getClientId()));
        url.append("&redirect_uri=").append(urlEncode(googleOAuthProperties.getRedirectUri()));
        url.append("&response_type=code");
        url.append("&scope=").append(urlEncode(GOOGLE_SCOPE));
        url.append("&access_type=offline");
        url.append("&prompt=consent");
        
        if (state != null && !state.isEmpty()) {
            url.append("&state=").append(urlEncode(state));
        }

        log.debug("Generated Google OAuth authorization URL");
        return url.toString();
    }

    @Override
    public GoogleTokenResponse exchangeCode(String code) {
        if (!googleOAuthProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Google OAuth is not configured. Please set client ID and secret.");
        }

        try {
            // Prepare request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("code", code);
            requestBody.add("client_id", googleOAuthProperties.getClientId());
            requestBody.add("client_secret", googleOAuthProperties.getClientSecret());
            requestBody.add("redirect_uri", googleOAuthProperties.getRedirectUri());
            requestBody.add("grant_type", "authorization_code");

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Make request to Google token endpoint
            log.debug("Exchanging authorization code for tokens");
            ResponseEntity<GoogleTokenResponse> response = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                request,
                GoogleTokenResponse.class
            );

            GoogleTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || tokenResponse.getIdToken() == null) {
                log.error("Invalid token response from Google: no id_token received");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                    "Failed to obtain ID token from Google");
            }

            log.info("Successfully exchanged authorization code for tokens");
            return tokenResponse;

        } catch (RestClientException e) {
            log.error("Failed to exchange authorization code with Google", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Invalid authorization code or Google service unavailable");
        }
    }

    @Override
    public GoogleUserInfo validateAndExtractUserInfo(String idToken) {
        try {
            // Parse JWT without signature verification (Google's signature verification requires fetching public keys)
            // In production, you should verify the signature using Google's public keys
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Invalid ID token format");
            }

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            
            // Parse JSON to extract claims
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            // Validate issuer
            String issuer = (String) claims.get("iss");
            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                log.error("Invalid token issuer: {}", issuer);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Invalid ID token issuer");
            }

            // Validate audience (client ID)
            String audience = (String) claims.get("aud");
            if (!googleOAuthProperties.getClientId().equals(audience)) {
                log.error("Invalid token audience: {}", audience);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Invalid ID token audience");
            }

            // Validate expiration
            Number exp = (Number) claims.get("exp");
            if (exp != null && exp.longValue() < System.currentTimeMillis() / 1000) {
                log.error("ID token has expired");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "ID token has expired");
            }

            // Extract user info
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setGoogleId((String) claims.get("sub"));
            userInfo.setEmail((String) claims.get("email"));
            userInfo.setEmailVerified((Boolean) claims.get("email_verified"));
            userInfo.setName((String) claims.get("name"));
            userInfo.setPicture((String) claims.get("picture"));
            userInfo.setGivenName((String) claims.get("given_name"));
            userInfo.setFamilyName((String) claims.get("family_name"));
            userInfo.setLocale((String) claims.get("locale"));

            log.info("Successfully validated and extracted user info from ID token: email={}", userInfo.getEmail());
            return userInfo;

        } catch (IOException e) {
            log.error("Failed to parse ID token", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to parse ID token");
        }
    }

    /**
     * Generate secure random state parameter for CSRF protection
     */
    public String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void storeState(String state) {
        String key = OAUTH_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, "valid", STATE_EXPIRATION_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        log.debug("Stored OAuth state in Redis: {}", state);
    }

    @Override
    public boolean validateState(String state) {
        if (state == null || state.isEmpty()) {
            log.warn("OAuth state is null or empty");
            return false;
        }

        String key = OAUTH_STATE_PREFIX + state;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // Delete the state after validation (one-time use)
            redisTemplate.delete(key);
            log.debug("OAuth state validated and removed: {}", state);
            return true;
        }
        
        log.warn("Invalid or expired OAuth state: {}", state);
        return false;
    }

    /**
     * URL encode a string
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
