package edu.zsc.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Hash utility class
 * 
 * @author zgq
 * @since 2025-10-02
 */
@Slf4j
public class HashUtil {
    
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Calculate SHA-256 hash value of byte array
     * 
     * @param data Byte array
     * @return 64-character hexadecimal string
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            log.error("Failed to calculate SHA-256 hash", e);
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }
    
    /**
     * Calculate SHA-256 hash value of input stream
     * 
     * @param inputStream Input stream
     * @return 64-character hexadecimal string
     */
    public static String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            log.error("Failed to calculate SHA-256 hash", e);
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }
    
    /**
     * Calculate SHA-256 hash value of string
     * 
     * @param text String
     * @return 64-character hexadecimal string
     */
    public static String sha256(String text) {
        return sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    /**
     * Convert byte array to hexadecimal string
     * 
     * @param bytes Byte array
     * @return Hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

