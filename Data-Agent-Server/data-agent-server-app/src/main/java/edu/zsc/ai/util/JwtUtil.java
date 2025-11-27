package edu.zsc.ai.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT Utility Class
 * Generate and parse JWT tokens
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:data-agent-secret-key-change-in-production-min-256-bits}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")
    private Long accessTokenExpiration; // 1 hour

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private Long refreshTokenExpiration; // 30 days

    /**
     * Generate Access Token
     */
    public String generateAccessToken(Long userId, String email) {
        return generateToken(userId, email, accessTokenExpiration);
    }

    /**
     * Generate token with custom claims
     */
    private String generateToken(Long userId, String email, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parse token and get claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Get email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get expiration time from token
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }
}
