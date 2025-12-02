package edu.zsc.ai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Password Encryption Utility
 * Uses AES encryption for database passwords
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
public class PasswordEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${app.security.encryption-key:data-agent-secret-key-for-password-encryption}")
    private String encryptionKey;

    /**
     * Encrypt password
     *
     * @param password Plain text password
     * @return Encrypted password (Base64 encoded)
     */
    public String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }

        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Failed to encrypt password", e);
            throw new RuntimeException("Password encryption failed", e);
        }
    }

    /**
     * Decrypt password
     *
     * @param encryptedPassword Encrypted password (Base64 encoded)
     * @return Plain text password
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }

        try {
            SecretKeySpec secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt password", e);
            throw new RuntimeException("Password decryption failed", e);
        }
    }

    /**
     * Generate AES key from encryption key
     */
    private SecretKeySpec generateKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = encryptionKey.getBytes(StandardCharsets.UTF_8);
        key = sha.digest(key);
        // Use first 128 bits (16 bytes) for AES-128
        byte[] keyBytes = new byte[16];
        System.arraycopy(key, 0, keyBytes, 0, 16);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
