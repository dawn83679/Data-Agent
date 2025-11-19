package edu.zsc.ai.service;

/**
 * Email Service Interface
 *
 * @author Data-Agent Team
 */
public interface EmailService {

    /**
     * Send verification code email
     *
     * @param to recipient email
     * @param code verification code
     * @param codeType code type
     */
    void sendVerificationCode(String to, String code, String codeType);
}
