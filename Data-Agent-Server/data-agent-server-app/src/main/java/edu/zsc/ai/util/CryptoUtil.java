package edu.zsc.ai.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

public final class CryptoUtil {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private CryptoUtil() {
    }

    // Password (BCrypt)
    public static boolean match(String rawPassword, String encodedPassword) {
        return BCRYPT.matches(rawPassword, encodedPassword);
    }

    public static String encode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    // Random token
    public static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a random secure password (typically for OAuth users)
     * Returns a 64-character random string
     */
    public static String generateRandomPassword() {
        return randomToken() + randomToken();
    }

    // SHA-256 (using Apache Commons Codec)
    public static String sha256Hex(String input) {
        return DigestUtils.sha256Hex(input);
    }
}
