package edu.zsc.ai.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // SHA-256
    public static String sha256Hex(String input)  {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();

    }
}


