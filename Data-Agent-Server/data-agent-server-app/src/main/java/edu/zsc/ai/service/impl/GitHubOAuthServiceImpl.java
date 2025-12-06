package edu.zsc.ai.service.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.zsc.ai.config.properties.GitHubOAuthProperties;
import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.GitHubTokenResponse;
import edu.zsc.ai.model.dto.response.GitHubUserInfo;
import edu.zsc.ai.service.GitHubOAuthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub OAuth Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthServiceImpl implements GitHubOAuthService {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RedisTemplate<String, Object> redisTemplate;
    
    private RestTemplate restTemplate;

    /**
     * Initialize RestTemplate with proxy configuration if enabled
     */
    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        
        // Configure proxy if enabled
        if (gitHubOAuthProperties.getProxy().isConfigured()) {
            String proxyHost = gitHubOAuthProperties.getProxy().getHost();
            int proxyPort = gitHubOAuthProperties.getProxy().getPort();
            
            Proxy proxy = new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(proxyHost, proxyPort)
            );
            requestFactory.setProxy(proxy);
            
            log.info("GitHub OAuth RestTemplate configured with proxy: {}:{}", proxyHost, proxyPort);
        } else {
            log.info("GitHub OAuth RestTemplate configured without proxy");
        }
        
        // Set connection and read timeouts
        requestFactory.setConnectTimeout(gitHubOAuthProperties.getConnectionTimeout());
        requestFactory.setReadTimeout(gitHubOAuthProperties.getReadTimeout());
        
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public String getAuthorizationUrl(String state) {
        if (!gitHubOAuthProperties.isConfigured()) {
            log.error("GitHub OAuth configuration is incomplete. Client ID: {}, Client Secret: {}, Redirect URI: {}, Frontend Redirect URI: {}",
                gitHubOAuthProperties.getClientId() != null ? "SET" : "NOT SET",
                gitHubOAuthProperties.getClientSecret() != null ? "SET" : "NOT SET",
                gitHubOAuthProperties.getRedirectUri() != null ? "SET" : "NOT SET",
                gitHubOAuthProperties.getFrontendRedirectUri() != null ? "SET" : "NOT SET");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "GitHub OAuth is not configured. Please set client ID, client secret, redirect URI, and frontend redirect URI.");
        }

        // Build authorization URL with required parameters
        StringBuilder url = new StringBuilder(gitHubOAuthProperties.getAuthUrl());
        url.append("?client_id=").append(urlEncode(gitHubOAuthProperties.getClientId()));
        url.append("&redirect_uri=").append(urlEncode(gitHubOAuthProperties.getRedirectUri()));
        url.append("&scope=").append(urlEncode(gitHubOAuthProperties.getScope()));
        
        if (state != null && !state.isEmpty()) {
            url.append("&state=").append(urlEncode(state));
        }

        log.debug("Generated GitHub OAuth authorization URL");
        return url.toString();
    }

    @Override
    public GitHubTokenResponse exchangeCode(String code) {
        if (!gitHubOAuthProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "GitHub OAuth is not configured. Please set client ID and secret.");
        }

        try {
            // Prepare request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", gitHubOAuthProperties.getClientId());
            requestBody.add("client_secret", gitHubOAuthProperties.getClientSecret());
            requestBody.add("code", code);
            requestBody.add("redirect_uri", gitHubOAuthProperties.getRedirectUri());

            // Prepare headers - GitHub requires Accept header for JSON response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Make request to GitHub token endpoint
            log.debug("Exchanging authorization code for access token");
            ResponseEntity<GitHubTokenResponse> response = restTemplate.exchange(
                gitHubOAuthProperties.getTokenUrl(),
                HttpMethod.POST,
                request,
                GitHubTokenResponse.class
            );

            GitHubTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("Invalid token response from GitHub: no access_token received");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                    "Failed to obtain access token from GitHub");
            }

            log.info("Successfully exchanged authorization code for access token");
            return tokenResponse;

        } catch (RestClientException e) {
            log.error("Failed to exchange authorization code with GitHub", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Invalid authorization code or GitHub service unavailable");
        }
    }

    @Override
    public GitHubUserInfo getUserInfo(String accessToken) {
        try {
            // Prepare headers with access token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Make request to GitHub user API
            log.debug("Fetching user information from GitHub");
            ResponseEntity<GitHubUserInfo> response = restTemplate.exchange(
                gitHubOAuthProperties.getUserApiUrl(),
                HttpMethod.GET,
                request,
                GitHubUserInfo.class
            );

            GitHubUserInfo userInfo = response.getBody();
            if (userInfo == null) {
                log.error("Invalid user info response from GitHub");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                    "Failed to fetch user information from GitHub");
            }

            log.info("Successfully fetched user info from GitHub: login={}", userInfo.getLogin());
            return userInfo;

        } catch (RestClientException e) {
            log.error("Failed to fetch user information from GitHub", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Failed to fetch user information from GitHub");
        }
    }

    @Override
    public String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void storeState(String state) {
        String key = gitHubOAuthProperties.getStatePrefix() + state;
        redisTemplate.opsForValue().set(key, "valid", gitHubOAuthProperties.getStateExpirationMinutes(), java.util.concurrent.TimeUnit.MINUTES);
        log.debug("Stored OAuth state in Redis: {}", state);
    }

    @Override
    public boolean validateState(String state) {
        if (state == null || state.isEmpty()) {
            log.warn("OAuth state is null or empty");
            return false;
        }

        String key = gitHubOAuthProperties.getStatePrefix() + state;
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
